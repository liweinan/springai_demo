# springai_demo

React + Spring Boot + Spring AI（DeepSeek Tool Calling）全栈学习项目。

通过「订票列表 + 自然语言聊天」演示 **ReAct**（Reason + Act）：大模型理解用户意图 → 调用 Java `@Tool` → 修改数据库 → 前端列表实时刷新。

> 仓库地址：[github.com/liweinan/springai_demo](https://github.com/liweinan/springai_demo)

**详细架构说明（自学向）** → [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 效果预览

| 初始状态（3 条可订票） | 聊天「我要订票」后 |
|:---:|:---:|
| ![初始页面](docs/screenshots/01-initial-page.png) | ![订票后](docs/screenshots/02-after-booking.png) |

| 聊天「取消订票」后 |
|:---:|
| ![取消后](docs/screenshots/03-after-cancel.png) |

截图由 Playwright 自动生成：`cd e2e && npm run capture-screenshots`

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | React 18、TypeScript、Vite |
| 后端 | Java 17、Spring Boot 3.4、Spring Data JPA、H2 |
| AI | Spring AI 1.0、DeepSeek `deepseek-chat`、Tool Calling |
| 测试 | Playwright E2E |

---

## 快速启动

### 1. 配置 API Key（必做）

从 [DeepSeek 开放平台](https://platform.deepseek.com/) 获取 Key，**仅通过环境变量注入，绝不写入代码或 Git**：

```bash
export DEEPSEEK_API_KEY=your-key-here
# 或复制 .env.example 为 .env 后填入（.env 已在 .gitignore 中）
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

→ `http://localhost:8080`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

→ `http://localhost:5173`

### 4. 健康检查

```bash
curl http://localhost:8080/api/health
# {"deepseekConfigured":true,"deepseekReachable":true}
```

---

## 架构一览

```
React 页面
  ├─ GET  /api/bookings?status=...     → 双栏列表
  └─ POST /api/chat                    → 聊天

Spring Boot
  ├─ BookingController / BookingService / BookingRepository  → 经典三层
  └─ ChatController → ChatService → ChatClient
                                        ↓ ReAct 循环
                                   BookingTools (@Tool)
                                        ↓
                                   BookingService（同一写入口）
                                        ↓
                                   DeepSeek API
```

**ReAct 由谁驱动**：Spring AI 内置 `ToolCallingAdvisor`，自动完成「推理 → 调工具 → 回传结果 → 生成回复」，无需手写 `while`。

**如何确认工具被调用**：后端日志出现 `[Tool 被调用] subscribeTicket`。

---

## Spring AI 四个核心文件

| 文件 | 作用 |
|------|------|
| `config/ChatConfig.java` | 组装 `ChatClient` + system prompt + tools |
| `tools/BookingTools.java` | 3 个 `@Tool` 方法（订票 / 取消 / 查列表） |
| `service/ChatService.java` | **唯一**调用大模型：`chatClient.prompt().user().call()` |
| `controller/ChatController.java` | HTTP 入口，不含 AI 逻辑 |

---

## API 一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/bookings?status=SUBSCRIBED` | 已订阅列表 |
| GET | `/api/bookings?status=UNSUBSCRIBED` | 未订阅列表 |
| POST | `/api/chat` | 聊天，`{"message":"我要订票"}` |
| GET | `/api/health` | Key 配置与 DeepSeek 连通性 |

---

## curl 验收（不启前端）

```bash
curl "http://localhost:8080/api/bookings?status=UNSUBSCRIBED"

curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"我要订票"}'

curl "http://localhost:8080/api/bookings?status=SUBSCRIBED"
```

---

## E2E 测试（Playwright）

```bash
export DEEPSEEK_API_KEY=your-key-here
cd e2e
npm install
npx playwright install chromium
npm test
```

| 用例 | 验证 |
|------|------|
| 初始加载 | 3 未订阅 / 0 已订阅 |
| 聊天订票 | 「我要订票」→ 左栏 +1 |
| 聊天取消 | 「取消订票 G123」→ 票回右栏 |
| 健康检查 | `/api/health` 正常 |

---

## 项目结构

```
springai_demo/
├── backend/          # Spring Boot + Spring AI
├── frontend/         # React + Vite
├── e2e/              # Playwright 测试 + 截图脚本
├── docs/screenshots/ # README 用截图
├── .env.example      # Key 配置示例（不含真实 Key）
└── README.md
```

---

## 可扩展方向

- 用户登录、SQLite 持久化、SSE 流式聊天、意图识别规则兜底

---

## License

MIT

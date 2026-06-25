# 开发模式：Vite dev server + 代理 /api → backend 容器
FROM node:22-alpine
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@9.15.9 --activate

# pnpm workspace：从仓库根目录复制锁文件与 frontend 包
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY frontend/package.json frontend/
RUN pnpm install --frozen-lockfile --filter frontend...

COPY frontend ./frontend
WORKDIR /app/frontend
EXPOSE 5173
CMD ["pnpm", "dev", "--host", "0.0.0.0", "--port", "5173"]

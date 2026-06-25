package com.demo.booking.service;

import com.demo.booking.advisor.PromptLoggingAdvisor;
import com.demo.booking.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 【AI 业务层】聊天服务，Spring AI 用法的集中展示点。
 * <p>
 * 本类是<strong>唯一调用大模型</strong>的地方。
 * 读懂 {@link #chat(String)} 方法，就理解了 Spring AI ChatClient 的基本用法。
 * </p>
 * <p>
 * 【Spring AI 2.0 调用链】一次 {@code call()} 内部可能触发多轮 DeepSeek 请求（ReAct），
 * 但业务代码仍只需写 {@code chatClient.prompt().user().call().content()}——
 * 循环由 {@code ToolCallingAdvisor} 驱动，逐步日志由 {@link PromptLoggingAdvisor} 输出。
 * </p>
 * <p>
 * 被 {@link com.demo.booking.controller.ChatController} 调用。
 * </p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 处理用户聊天消息，驱动 ReAct 循环完成订票/取消等操作。
     * <p>
     * 调用链（Spring AI 2.0，Advisor 链内 ReAct）：
     * <pre>
     * chatClient.prompt().user(message).call().content()
     *   → PromptLoggingAdvisor 第 1 步：Prompt → DeepSeek → Response（含 tool_calls）
     *   → ToolCallingAdvisor 执行 BookingTools
     *   → PromptLoggingAdvisor 第 2 步：Prompt（含 TOOL_RESPONSE）→ DeepSeek → 最终文本
     * </pre>
     * 1.x 中上述中间步骤发生在 ChatModel 内部，控制台通常看不到第 1 步的 tool_calls 与第 2 步的 TOOL_RESPONSE。
     * </p>
     *
     * @param userMessage 用户自然语言输入
     * @return 成功或失败的 ChatResponse
     */
    public ChatResponse chat(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return ChatResponse.failure("请输入消息", "message 不能为空");
        }

        log.info("[Chat] 收到用户消息: {}", userMessage);

        try {
            PromptLoggingAdvisor.resetSteps();  // 2.0：一次 HTTP 请求可能对应多步 Advisor 迭代
            // 单次 call()；ToolCallingAdvisor 在链内完成 ReAct，PromptLoggingAdvisor 记录每步 Prompt/Response
            String reply = chatClient
                    .prompt()
                    .user(userMessage.trim())
                    .call()
                    .content();

            log.info("[Chat] AI 回复: {}", reply);
            return ChatResponse.success(reply);
        } catch (Exception ex) {
            log.error("[Chat] 调用 DeepSeek 失败", ex);
            return ChatResponse.failure(
                    "抱歉，AI 服务暂时不可用，请检查 DEEPSEEK_API_KEY 和网络连接。",
                    ex.getMessage()
            );
        }
    }

    /**
     * 探测 DeepSeek 是否可达（用于 /api/health）。
     * 发送极简 prompt，成功即表示 Key 和网络正常。
     *
     * @return true 表示连通
     */
    public boolean pingDeepSeek() {
        try {
            chatClient.prompt().user("ping").call().content();
            return true;
        } catch (Exception ex) {
            log.warn("[Chat] DeepSeek 连通性检测失败: {}", ex.getMessage());
            return false;
        }
    }
}

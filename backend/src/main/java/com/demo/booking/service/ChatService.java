package com.demo.booking.service;

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
     * 调用链：
     * <pre>
     * chatClient.prompt().user(message).call().content()
     *   → DeepSeek 推理
     *   → ToolCallingAdvisor 执行 BookingTools
     *   → 再次调用 DeepSeek 生成最终回复
     * </pre>
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
            // 第 1 步：把用户消息 + system prompt + 工具清单 发给 DeepSeek
            // 第 2 步：call() 内部 ToolCallingAdvisor 自动完成 ReAct 循环
            // 第 3 步：content() 取模型最终文本回复
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

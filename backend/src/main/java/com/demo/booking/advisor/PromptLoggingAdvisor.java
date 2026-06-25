package com.demo.booking.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.Ordered;

/**
 * 在 ToolCallingAdvisor 循环内部记录每一次发给大模型的 Prompt 与模型返回的 Response。
 * <p>
 * 【日志分两层】
 * <ul>
 *   <li><b>INFO</b> — {@code prompt.getInstructions()} 中的对话消息（SYSTEM / USER / ASSISTANT / TOOL_RESPONSE）</li>
 *   <li><b>DEBUG</b> — {@code prompt.getOptions()} 里注册的工具定义（name / description / inputSchema）</li>
 * </ul>
 * 工具<strong>不会</strong>出现在 INFO 的 messages 列表里：Spring AI 把 {@code @Tool} 注册结果放进
 * {@link ToolCallingChatOptions#getToolCallbacks()}，由 {@code DeepSeekChatModel} 转成 HTTP 请求体中的
 * {@code tools} 字段，与 {@code messages} 并列，而非一条 SYSTEM 文本。因此仅看 INFO 会误以为「没注册工具」。
 * </p>
 * <p>
 * Docker Compose 默认将本类与 {@code org.springframework.ai} 设为 DEBUG，便于观察完整请求结构。
 * 本地 {@code mvn spring-boot:run} 默认 INFO；需要工具明细时可临时加
 * {@code logging.level.com.demo.booking.advisor.PromptLoggingAdvisor=DEBUG}。
 * </p>
 */
public class PromptLoggingAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(PromptLoggingAdvisor.class);

    private static final ThreadLocal<Integer> STEP = ThreadLocal.withInitial(() -> 0);

    /**
     * 每次用户聊天开始前调用，重置步骤计数。
     */
    public static void resetSteps() {
        STEP.set(0);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        int step = STEP.get() + 1;
        STEP.set(step);
        log.info("[AI 第{}步] 发送 Prompt（messages）:\n{}", step, formatMessages(request));
        log.debug("[AI 第{}步] 注册的工具（options → API tools 字段，非 messages）:\n{}",
                step, formatRegisteredTools(request));
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        log.info("[AI 第{}步] 收到 Response:\n{}", STEP.get(), formatResponse(response));
        return response;
    }

    /**
     * Advisor 链 order：必须 &gt; ToolCallingAdvisor（+300）才能进入工具循环内部。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 400;
    }

    /**
     * 对话消息列表 — 对应 DeepSeek 请求 JSON 中的 {@code messages} 数组。
     */
    private static String formatMessages(ChatClientRequest request) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Message message : request.prompt().getInstructions()) {
            builder.append("  [").append(index++).append("] ").append(formatMessage(message)).append('\n');
        }
        return builder.toString().stripTrailing();
    }

    /**
     * 工具定义 — 对应 DeepSeek 请求 JSON 中的 {@code tools} 数组（function calling schema）。
     */
    private static String formatRegisteredTools(ChatClientRequest request) {
        ChatOptions options = request.prompt().getOptions();
        if (!(options instanceof ToolCallingChatOptions toolOptions)) {
            return "  (无 ToolCallingChatOptions，未注册工具)";
        }
        var toolCallbacks = toolOptions.getToolCallbacks();
        if (toolCallbacks == null || toolCallbacks.isEmpty()) {
            return "  (toolCallbacks 为空)";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (ToolCallback callback : toolCallbacks) {
            ToolDefinition definition = callback.getToolDefinition();
            builder.append("  [").append(index++).append("] ")
                    .append(definition.name()).append('\n')
                    .append("      description: ").append(definition.description()).append('\n')
                    .append("      inputSchema: ").append(definition.inputSchema()).append('\n');
        }
        return builder.toString().stripTrailing();
    }

    private static String formatMessage(Message message) {
        if (message instanceof SystemMessage system) {
            return "SYSTEM: " + system.getText();
        }
        if (message instanceof UserMessage user) {
            return "USER: " + user.getText();
        }
        if (message instanceof AssistantMessage assistant) {
            return formatAssistantMessage(assistant);
        }
        if (message instanceof ToolResponseMessage toolResponse) {
            return formatToolResponseMessage(toolResponse);
        }
        return message.getClass().getSimpleName() + ": " + message;
    }

    private static String formatAssistantMessage(AssistantMessage assistant) {
        StringBuilder builder = new StringBuilder("ASSISTANT");
        if (assistant.hasToolCalls()) {
            builder.append(" (tool_calls)");
            for (AssistantMessage.ToolCall toolCall : assistant.getToolCalls()) {
                builder.append("\n      - ")
                        .append(toolCall.name())
                        .append("(")
                        .append(toolCall.arguments())
                        .append(")");
            }
        }
        if (assistant.getText() != null && !assistant.getText().isBlank()) {
            builder.append(": ").append(assistant.getText());
        }
        return builder.toString();
    }

    private static String formatToolResponseMessage(ToolResponseMessage toolResponse) {
        StringBuilder builder = new StringBuilder("TOOL_RESPONSE");
        for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
            builder.append("\n      - ")
                    .append(response.name())
                    .append(" => ")
                    .append(response.responseData());
        }
        return builder.toString();
    }

    private static String formatResponse(ChatClientResponse clientResponse) {
        ChatResponse chatResponse = clientResponse.chatResponse();
        if (chatResponse == null) {
            return "(empty)";
        }
        StringBuilder builder = new StringBuilder();
        chatResponse.getResults().forEach(result -> {
            Message output = result.getOutput();
            builder.append(formatMessage(output));
            if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
                builder.append("\n  metadata: ").append(result.getMetadata());
            }
        });
        return builder.toString().stripTrailing();
    }
}

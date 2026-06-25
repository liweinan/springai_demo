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
import org.springframework.core.Ordered;

/**
 * 在 ToolCallingAdvisor 循环内部记录每一次发给大模型的 Prompt 与模型返回的 Response。
 * <p>
 * 【为何需要 Spring AI 2.0】
 * 1.x 中工具循环在 {@code ChatModel.internalCall()} 内递归，Advisor 链通常只能看到首尾各一次请求/响应。
 * 2.0 把循环放进 {@code ToolCallingAdvisor}，order 大于其默认值（+300）的 Advisor 会参与<strong>每一轮</strong>迭代。
 * </p>
 * <p>
 * 本类 order = {@link Ordered#HIGHEST_PRECEDENCE} + 400，因此日志中会出现
 * {@code [AI 第1步]}（模型决定调工具）、{@code [AI 第2步]}（携带 TOOL_RESPONSE 后生成最终回复）等。
 * </p>
 * <p>
 * 1.x 时代可用 {@code SimpleLoggerAdvisor}，但无法逐步观察 ReAct；2.0 的 Advisor 链模式才使本类有意义。
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
        log.info("[AI 第{}步] 发送 Prompt:\n{}", step, formatPrompt(request));
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        log.info("[AI 第{}步] 收到 Response:\n{}", STEP.get(), formatResponse(response));
        return response;
    }

    /**
     * Advisor 链 order：必须 &gt; ToolCallingAdvisor（+300）才能进入工具循环内部。
     * 数值越小越靠外层；+400 保证在每一轮 model call 前后都能 before/after 打日志。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 400;
    }

    private static String formatPrompt(ChatClientRequest request) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Message message : request.prompt().getInstructions()) {
            builder.append("  [").append(index++).append("] ").append(formatMessage(message)).append('\n');
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

package com.demo.booking.config;

import com.demo.booking.advisor.PromptLoggingAdvisor;
import com.demo.booking.tools.BookingTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 核心配置：组装 {@link ChatClient}。
 * <p>
 * 【Spring AI 2.0 相对 1.x 的变化】
 * <ul>
 *   <li>需配合 Spring Boot 4.x（本项目：Boot 4.1 + AI 2.0）</li>
 *   <li>{@code defaultTools(...)} 后框架<strong>自动注册</strong> {@code ToolCallingAdvisor}（order ≈ +300），
 *       1.x 需手动注册 {@code ToolCallAdvisor} 且工具循环多在 {@code ChatModel} 内部，外层 Advisor 难以逐步观测</li>
 *   <li>ReAct 循环在 Advisor 链内执行，自定义 Advisor 可通过 order 插入循环内部（见 {@link PromptLoggingAdvisor}）</li>
 *   <li>{@code ToolCallingManager} 仍由 Boot 自动配置，本类无需注入</li>
 * </ul>
 * </p>
 * <p>
 * 【ReAct 原理简述】
 * <ol>
 *   <li>注册 {@code defaultTools(bookingTools)} → 自动启用 {@code ToolCallingAdvisor}</li>
 *   <li>用户发消息 → system + 用户消息进入 {@code messages}；{@code @Tool} 定义进入 {@code options.toolCallbacks} → API {@code tools}</li>
 *   <li>模型返回 tool call → ToolCallingAdvisor 执行 {@link BookingTools} 中对应方法</li>
 *   <li>工具结果回传模型 → 模型生成最终中文回复</li>
 * </ol>
 * {@link PromptLoggingAdvisor}（order +400）打印每步 messages（INFO）与 tool 定义（DEBUG，见该类注释）。
 * </p>
 */
@Configuration
public class ChatConfig {

    /**
     * 创建全局 ChatClient Bean。
     * <p>
     * {@link com.demo.booking.service.ChatService} 是唯一注入并使用它的地方。
     * ToolCallingAdvisor 由 Spring AI 2.0 自动注册，无需手动添加。
     * 若再显式 {@code ToolCallingAdvisor.builder()...} 会导致链中出现两个工具 Advisor，应避免。
     * </p>
     *
     * @param chatModel    Spring AI 自动配置的 DeepSeekChatModel
     * @param bookingTools 订票工具，供大模型在 ReAct 中调用
     * @return 配置好 system prompt、tools 与日志 Advisor 的 ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, BookingTools bookingTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是订票助手。必须严格遵守：
                        1. 禁止编造订票/取消结果；所有订票、取消、查票必须调用工具，禁止未调工具就声称成功或失败。
                        2. 用户说「我要订票」或要订票 → 立即调用 subscribeTicket（title 可省略，会订第一张未订阅票）。
                        3. 用户要取消订票 → 必须调用 cancelSubscription，title 传用户提到的关键词（如 G123、北京、北京到上海）。
                           若用户未指明票名 → 先 listSubscribedTickets；若仅一张已订阅，用该票关键词调用 cancelSubscription。
                           禁止在未调工具的情况下说「无法取消」或「没有订阅」。
                        4. 用户问可订列表 → listUnsubscribedTickets；问已订有哪些 → listSubscribedTickets。
                        5. 工具成功后用一句简洁中文回复。
                        """)
                .defaultTools(bookingTools)   // 工具 schema 写入 options.toolCallbacks，非 SYSTEM 文本
                .defaultAdvisors(new PromptLoggingAdvisor())  // INFO=messages，DEBUG=tools
                .build();
    }
}

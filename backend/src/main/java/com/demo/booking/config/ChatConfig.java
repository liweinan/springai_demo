package com.demo.booking.config;

import com.demo.booking.tools.BookingTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 核心配置：组装 {@link ChatClient}。
 * <p>
 * 【ReAct 原理简述】
 * <ol>
 *   <li>注册 {@code defaultTools(bookingTools)} 后，Spring AI 自动启用 {@code ToolCallingAdvisor}</li>
 *   <li>用户发消息 → ChatClient 把 system prompt + 工具清单 + 用户消息发给 DeepSeek</li>
 *   <li>模型返回 tool call → Advisor 自动执行 {@link BookingTools} 中对应方法</li>
 *   <li>工具结果回传模型 → 模型生成最终中文回复</li>
 * </ol>
 * 以上循环由框架完成，无需手写 {@code while}。
 * </p>
 */
@Configuration
public class ChatConfig {

    /**
     * 创建全局 ChatClient Bean。
     * <p>
     * {@link com.demo.booking.service.ChatService} 是唯一注入并使用它的地方。
     * </p>
     *
     * @param chatModel    Spring AI 自动配置的 DeepSeekChatModel
     * @param bookingTools 订票工具，供大模型在 ReAct 中调用
     * @return 配置好 system prompt 和 tools 的 ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, BookingTools bookingTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是订票助手。必须严格遵守：
                        1. 禁止编造订票结果；所有订票/取消/查票必须调用工具完成。
                        2. 用户说「我要订票」或要订票 → 立即调用 subscribeTicket（title 可传 null 或省略，会订第一张未订阅票），不要只列出票让用户选。
                        3. 用户说「取消订票 xxx」→ 调用 cancelSubscription，title 用用户提到的关键词。
                        4. 用户明确问「有哪些票可以订」→ 调用 listUnsubscribedTickets 后回答。
                        5. 工具成功后用一句简洁中文回复。
                        """)
                .defaultTools(bookingTools)
                .build();
    }
}

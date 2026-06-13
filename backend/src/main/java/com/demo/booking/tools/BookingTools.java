package com.demo.booking.tools;

import com.demo.booking.dto.BookingResponse;
import com.demo.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 【AI 工具层】暴露给大模型调用的 Java 方法。
 * <p>
 * Spring AI 通过 {@code @Tool} 注解把这些方法注册为「工具」，
 * DeepSeek 在 ReAct 循环中会决定何时调用它们。
 * </p>
 * <p>
 * <b>设计原则</b>：每个工具方法体内只调用 {@link BookingService}，
 * 不直接操作数据库，保证与 REST API 数据一致。
 * </p>
 * <p>
 * 学习时在控制台搜索 {@code [Tool 被调用]}，可观察 ReAct 是否真正发生。
 * </p>
 */
@Component
public class BookingTools {

    private static final Logger log = LoggerFactory.getLogger(BookingTools.class);

    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 大模型何时调用：用户问「有哪些票可以订」「查一下未订阅的票」等。
     *
     * @return 所有未订阅（可订）的票
     */
    @Tool(description = "查询当前所有未订阅（可订）的票。用户询问可订列表时调用。")
    public List<BookingResponse> listUnsubscribedTickets() {
        log.info("[Tool 被调用] listUnsubscribedTickets");
        return bookingService.listUnsubscribedTickets();
    }

    /**
     * 大模型何时调用：用户说「我要订票」「帮我订 G123」等。
     *
     * @param title 票名关键词，可选；不传则订第一张未订阅票
     * @return 订阅成功后的票信息
     */
    @Tool(description = "订阅一张票（订票）。title 可选：不传则订第一张未订阅票；传则模糊匹配未订阅票，匹配不到则新建。")
    public BookingResponse subscribeTicket(String title) {
        log.info("[Tool 被调用] subscribeTicket, title={}", title);
        return bookingService.subscribeTicket(title);
    }

    /**
     * 大模型何时调用：用户说「取消订票 xxx」「退订 G123」等。
     *
     * @param title 票名关键词，必填，支持模糊匹配
     * @return 取消后的票信息
     */
    @Tool(description = "取消已订阅的票。title 必填，在已订阅票中模糊匹配。")
    public BookingResponse cancelSubscription(String title) {
        log.info("[Tool 被调用] cancelSubscription, title={}", title);
        return bookingService.cancelSubscription(title);
    }
}

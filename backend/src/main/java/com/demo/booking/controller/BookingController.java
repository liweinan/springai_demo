package com.demo.booking.controller;

import com.demo.booking.dto.BookingResponse;
import com.demo.booking.model.BookingStatus;
import com.demo.booking.service.BookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 【表现层】订票 REST API。
 * <p>
 * 只负责：接收 HTTP 请求 → 调用 Service → 返回 JSON。
 * 不写任何业务逻辑。
 * </p>
 * <p>
 * 前端双栏列表通过本 Controller 的两个 GET 请求拉取数据。
 * </p>
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 查询订票列表。
     * <p>
     * 示例：
     * <ul>
     *   <li>{@code GET /api/bookings?status=SUBSCRIBED} — 已订阅（左栏）</li>
     *   <li>{@code GET /api/bookings?status=UNSUBSCRIBED} — 未订阅（右栏）</li>
     * </ul>
     * </p>
     *
     * @param status 必填，SUBSCRIBED 或 UNSUBSCRIBED
     * @return 订票 JSON 数组
     */
    @GetMapping
    public List<BookingResponse> listBookings(@RequestParam BookingStatus status) {
        return bookingService.listByStatus(status);
    }
}

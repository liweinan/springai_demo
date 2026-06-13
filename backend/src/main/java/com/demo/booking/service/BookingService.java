package com.demo.booking.service;

import com.demo.booking.dto.BookingResponse;
import com.demo.booking.model.Booking;
import com.demo.booking.model.BookingStatus;
import com.demo.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 【业务层】订票核心业务逻辑。
 * <p>
 * 职责：
 * <ul>
 *   <li>查询已订阅 / 未订阅列表</li>
 *   <li>订阅（订票）与取消订阅</li>
 * </ul>
 * </p>
 * <p>
 * <b>重要</b>：所有写操作（订票、取消）的唯一入口。
 * REST API 和 AI {@code @Tool} 都调用本类，保证数据一致。
 * </p>
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * 按状态查询订票列表。
     *
     * @param status SUBSCRIBED 或 UNSUBSCRIBED
     * @return DTO 列表，供 Controller 返回 JSON
     */
    public List<BookingResponse> listByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /**
     * 查询所有未订阅（可订）的票。
     * 供 AI 工具 {@code listUnsubscribedTickets} 调用。
     *
     * @return 未订阅票列表
     */
    public List<BookingResponse> listUnsubscribedTickets() {
        return listByStatus(BookingStatus.UNSUBSCRIBED);
    }

    /**
     * 订阅一张票（订票）。
     * <p>
     * 业务规则（按顺序）：
     * <ol>
     *   <li>若 title 有值 → 在未订阅票中模糊匹配 title</li>
     *   <li>匹配到 → 将该票状态改为 SUBSCRIBED</li>
     *   <li>未匹配到且 title 有值 → 新建一条已订阅票</li>
     *   <li>title 为空 → 订第一张未订阅票（兜底，降低 AI 传参不准的失败率）</li>
     * </ol>
     * </p>
     *
     * @param title 票名关键词，可为 null 或空
     * @return 订阅后的票信息
     * @throws IllegalStateException 没有可订的票且未指定 title
     */
    @Transactional
    public BookingResponse subscribeTicket(String title) {
        // 步骤 1：若指定了 title，先在未订阅票中模糊匹配
        if (StringUtils.hasText(title)) {
            List<Booking> matched = bookingRepository
                    .findByStatusAndTitleContainingIgnoreCase(BookingStatus.UNSUBSCRIBED, title.trim());
            if (!matched.isEmpty()) {
                Booking booking = matched.get(0);
                booking.setStatus(BookingStatus.SUBSCRIBED);
                return BookingResponse.from(bookingRepository.save(booking));
            }
            // 匹配不到：创建一张新的已订阅票
            Booking newBooking = new Booking(title.trim(), BookingStatus.SUBSCRIBED);
            return BookingResponse.from(bookingRepository.save(newBooking));
        }

        // 步骤 2：未指定 title → 订第一张未订阅票
        Booking firstUnsubscribed = bookingRepository
                .findFirstByStatusOrderByIdAsc(BookingStatus.UNSUBSCRIBED)
                .orElseThrow(() -> new IllegalStateException("当前没有可订阅的票，请先取消已有订阅或指定票名新建"));

        firstUnsubscribed.setStatus(BookingStatus.SUBSCRIBED);
        return BookingResponse.from(bookingRepository.save(firstUnsubscribed));
    }

    /**
     * 取消已订阅的票。
     * <p>
     * 在已订阅票中按 title 模糊匹配，找到后改回 UNSUBSCRIBED。
     * </p>
     *
     * @param title 票名关键词，必填
     * @return 取消后的票信息
     * @throws IllegalArgumentException title 为空
     * @throws IllegalStateException    找不到匹配的已订阅票
     */
    @Transactional
    public BookingResponse cancelSubscription(String title) {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("取消订票必须提供票名关键词");
        }

        List<Booking> matched = bookingRepository
                .findByStatusAndTitleContainingIgnoreCase(BookingStatus.SUBSCRIBED, title.trim());

        if (matched.isEmpty()) {
            throw new IllegalStateException("未找到已订阅的票：" + title.trim());
        }

        Booking booking = matched.get(0);
        booking.setStatus(BookingStatus.UNSUBSCRIBED);
        return BookingResponse.from(bookingRepository.save(booking));
    }
}

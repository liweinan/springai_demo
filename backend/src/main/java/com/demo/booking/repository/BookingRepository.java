package com.demo.booking.repository;

import com.demo.booking.model.Booking;
import com.demo.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 【数据层】订票 JPA 仓库。
 * <p>
 * 继承 {@link JpaRepository} 后，Spring Data JPA 会自动实现常用 CRUD 方法。
 * 自定义方法名会按命名规则自动生成 SQL，无需手写 SQL。
 * </p>
 * <p>
 * 被 {@link com.demo.booking.service.BookingService} 调用，是数据库访问的唯一入口。
 * </p>
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * 按状态查询所有票。
     *
     * @param status SUBSCRIBED 或 UNSUBSCRIBED
     * @return 匹配的订票列表
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * 按状态 + 标题模糊匹配（忽略大小写）。
     * 用于「订 G123」「取消订票 北京-上海」等场景。
     *
     * @param status 限定在哪个状态集合里找
     * @param title  标题关键词
     * @return 匹配的订票列表
     */
    List<Booking> findByStatusAndTitleContainingIgnoreCase(BookingStatus status, String title);

    /**
     * 取指定状态下 id 最小的第一条记录。
     * 用于「我要订票」且未指定具体票名时，订第一张未订阅票。
     *
     * @param status 通常为 UNSUBSCRIBED
     * @return 第一条票，可能为空
     */
    Optional<Booking> findFirstByStatusOrderByIdAsc(BookingStatus status);
}

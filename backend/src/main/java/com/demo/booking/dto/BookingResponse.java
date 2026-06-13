package com.demo.booking.dto;

import com.demo.booking.model.Booking;
import com.demo.booking.model.BookingStatus;

/**
 * 【传输对象】订票信息的 JSON 响应格式。
 * <p>
 * 与 {@link Booking} 实体分离，避免直接把 JPA 实体暴露给前端，
 * 也便于以后扩展字段而不改数据库表。
 * </p>
 */
public class BookingResponse {

    private Long id;
    private String title;
    private BookingStatus status;

    public BookingResponse() {
    }

    public BookingResponse(Long id, String title, BookingStatus status) {
        this.id = id;
        this.title = title;
        this.status = status;
    }

    /**
     * 从实体转换为 DTO，供 Controller 和 @Tool 方法返回。
     *
     * @param booking JPA 实体
     * @return JSON 友好的响应对象
     */
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getTitle(), booking.getStatus());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}

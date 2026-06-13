package com.demo.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 订票实体，对应数据库表 {@code bookings}。
 * <p>
 * 【数据层】只描述数据结构，不含业务逻辑。
 * 字段刻意保持简单（id / title / status），便于学习。
 * </p>
 */
@Entity
@Table(name = "bookings")
public class Booking {

    /** 自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 票名，例如「北京-上海 G123」 */
    @Column(nullable = false)
    private String title;

    /** 订阅状态：SUBSCRIBED 或 UNSUBSCRIBED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    public Booking() {
    }

    public Booking(String title, BookingStatus status) {
        this.title = title;
        this.status = status;
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

package com.demo.booking.model;

/**
 * 订票状态枚举。
 * <ul>
 *   <li>{@link #SUBSCRIBED} — 已订阅（用户已订的票，显示在左栏）</li>
 *   <li>{@link #UNSUBSCRIBED} — 未订阅（可订但未订的票，显示在右栏）</li>
 * </ul>
 */
public enum BookingStatus {

    /** 已订阅：用户已成功订票 */
    SUBSCRIBED,

    /** 未订阅：票仍可被订阅 */
    UNSUBSCRIBED
}

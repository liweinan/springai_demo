/**
 * 订票列表相关 API。
 * 被 App.tsx 调用，拉取已订阅 / 未订阅两栏数据。
 */

import { getJson } from './client'
import type { Booking, BookingStatus } from '../types/booking'

/**
 * 按状态查询订票列表。
 *
 * @param status SUBSCRIBED（已订阅）或 UNSUBSCRIBED（未订阅）
 */
export function fetchBookings(status: BookingStatus): Promise<Booking[]> {
  return getJson<Booking[]>(`/api/bookings?status=${status}`)
}

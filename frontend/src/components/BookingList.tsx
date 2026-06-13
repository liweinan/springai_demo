/**
 * 双栏订票列表组件（纯展示）。
 *
 * 职责：展示「已订阅」和「未订阅」两栏票列表。
 * 被 App.tsx 调用，不包含任何业务逻辑或 API 调用。
 */

import type { Booking } from '../types/booking'

interface BookingListProps {
  /** 已订阅的票（左栏） */
  subscribed: Booking[]
  /** 未订阅的票（右栏） */
  unsubscribed: Booking[]
  /** 列表加载中 */
  loading: boolean
}

export default function BookingList({
  subscribed,
  unsubscribed,
  loading,
}: BookingListProps) {
  return (
    <section className="booking-lists">
      {/* 左栏：已订阅 */}
      <div className="booking-column">
        <h2>已订阅 ({subscribed.length})</h2>
        {loading ? (
          <p className="hint">加载中...</p>
        ) : subscribed.length === 0 ? (
          <p className="hint">暂无已订阅的票，可在下方聊天窗口说「我要订票」</p>
        ) : (
          <ul>
            {subscribed.map((booking) => (
              <li key={booking.id} className="booking-item subscribed">
                {booking.title}
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* 右栏：未订阅（可订） */}
      <div className="booking-column">
        <h2>未订阅 ({unsubscribed.length})</h2>
        {loading ? (
          <p className="hint">加载中...</p>
        ) : unsubscribed.length === 0 ? (
          <p className="hint">暂无可订的票</p>
        ) : (
          <ul>
            {unsubscribed.map((booking) => (
              <li key={booking.id} className="booking-item unsubscribed">
                {booking.title}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

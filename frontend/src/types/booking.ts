/**
 * 订票相关 TypeScript 类型定义。
 * 与后端 JSON 字段保持一致，便于类型检查。
 */

/** 订票状态，与后端 BookingStatus 枚举对应 */
export type BookingStatus = 'SUBSCRIBED' | 'UNSUBSCRIBED'

/** 单条订票记录 */
export interface Booking {
  id: number
  title: string
  status: BookingStatus
}

/** 聊天消息（前端本地状态） */
export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

/** POST /api/chat 响应 */
export interface ChatResponse {
  reply: string
  error?: string | null
}

/** GET /api/health 响应 */
export interface HealthResponse {
  deepseekConfigured: boolean
  deepseekReachable: boolean
}

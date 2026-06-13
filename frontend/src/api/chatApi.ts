/**
 * 聊天相关 API。
 * 被 ChatPanel / App.tsx 调用，发送用户消息给 Spring AI 后端。
 */

import { postJson } from './client'
import type { ChatResponse } from '../types/booking'

/**
 * 发送聊天消息到后端，触发 Spring AI ReAct 循环。
 *
 * @param message 用户自然语言，例如「我要订票」
 */
export function sendChatMessage(message: string): Promise<ChatResponse> {
  return postJson<ChatResponse>('/api/chat', { message })
}

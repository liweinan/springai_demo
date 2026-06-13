/**
 * 页面总控组件（整个应用只有这一个页面，无路由）。
 *
 * 职责：
 * 1. 挂载时拉取订票列表（已订阅 + 未订阅）
 * 2. 处理聊天发送 → 调用后端 → 刷新列表
 *
 * 状态全部用 useState，无 Redux，便于学习。
 */

import { useCallback, useEffect, useState } from 'react'
import { fetchBookings } from './api/bookingApi'
import { sendChatMessage } from './api/chatApi'
import BookingList from './components/BookingList'
import ChatPanel from './components/ChatPanel'
import type { Booking, ChatMessage } from './types/booking'
import './App.css'

function App() {
  // 已订阅票列表（左栏）
  const [subscribed, setSubscribed] = useState<Booking[]>([])
  // 未订阅票列表（右栏）
  const [unsubscribed, setUnsubscribed] = useState<Booking[]>([])
  // 列表是否正在加载
  const [loading, setLoading] = useState(true)
  // 聊天消息历史
  const [messages, setMessages] = useState<ChatMessage[]>([])
  // 是否正在等待 AI 回复
  const [sending, setSending] = useState(false)
  // 全局错误提示（列表或聊天失败）
  const [error, setError] = useState<string | null>(null)

  /**
   * 从后端拉取两栏列表数据。
   * 挂载时调用一次；聊天成功后也会再次调用以刷新 UI。
   */
  const loadBookings = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      // 并行请求两栏，减少等待时间
      const [subscribedData, unsubscribedData] = await Promise.all([
        fetchBookings('SUBSCRIBED'),
        fetchBookings('UNSUBSCRIBED'),
      ])
      setSubscribed(subscribedData)
      setUnsubscribed(unsubscribedData)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  // 组件挂载时加载列表
  useEffect(() => {
    void loadBookings()
  }, [loadBookings])

  /**
   * 处理用户发送聊天消息。
   * 调用链：ChatPanel → 本方法 → chatApi → 后端 ChatService → Spring AI ReAct
   * 成功后刷新列表，使订票/取消立即反映在双栏中。
   */
  const handleSendMessage = async (message: string) => {
    setSending(true)
    setError(null)

    // 先把用户消息显示在聊天窗口
    setMessages((prev) => [...prev, { role: 'user', content: message }])

    try {
      const response = await sendChatMessage(message)

      // 显示 AI 回复
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: response.reply },
      ])

      // 若后端返回 error 字段（如 Key 无效），提示用户
      if (response.error) {
        setError(response.error)
      }

      // 聊天可能已通过 @Tool 改了数据库，重新拉列表
      await loadBookings()
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送失败')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>订票聊天 Fullstack 学习项目</h1>
        <p>React 前端 + Spring Boot 后端 + Spring AI DeepSeek Tool Calling</p>
      </header>

      <BookingList
        subscribed={subscribed}
        unsubscribed={unsubscribed}
        loading={loading}
      />

      <ChatPanel
        messages={messages}
        sending={sending}
        error={error}
        onSend={handleSendMessage}
      />
    </div>
  )
}

export default App

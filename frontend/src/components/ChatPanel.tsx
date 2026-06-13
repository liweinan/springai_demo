/**
 * 聊天窗口组件。
 *
 * 职责：展示消息历史、输入框、发送按钮。
 * 被 App.tsx 调用；发送逻辑由 App 传入的 onSend 处理（便于发送后刷新列表）。
 */

import { useState } from 'react'
import type { ChatMessage } from '../types/booking'

interface ChatPanelProps {
  /** 消息历史（user / assistant） */
  messages: ChatMessage[]
  /** 是否正在等待 AI 回复 */
  sending: boolean
  /** 错误提示文字，无错误时为 null */
  error: string | null
  /** 用户点击发送时回调，由 App.tsx 调用 chatApi 并刷新列表 */
  onSend: (message: string) => Promise<void>
}

export default function ChatPanel({
  messages,
  sending,
  error,
  onSend,
}: ChatPanelProps) {
  // 输入框当前文字
  const [input, setInput] = useState('')

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = input.trim()
    if (!trimmed || sending) {
      return
    }
    setInput('')
    await onSend(trimmed)
  }

  return (
    <section className="chat-panel">
      <h2>聊天窗口</h2>
      <p className="chat-hint">
        试试：「我要订票」「取消订票 G123」「有哪些票可以订？」
      </p>

      {/* 消息历史区域 */}
      <div className="chat-messages">
        {messages.length === 0 ? (
          <p className="hint">发送消息开始对话...</p>
        ) : (
          messages.map((msg, index) => (
            <div
              key={`${msg.role}-${index}`}
              className={`chat-message ${msg.role}`}
            >
              <span className="chat-role">
                {msg.role === 'user' ? '你' : 'AI'}：
              </span>
              {msg.content}
            </div>
          ))
        )}
      </div>

      {/* 错误提示 */}
      {error && <p className="chat-error">{error}</p>}

      {/* 输入框 + 发送按钮 */}
      <form className="chat-form" onSubmit={handleSubmit}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="输入消息，例如：我要订票"
          disabled={sending}
        />
        <button type="submit" disabled={sending || !input.trim()}>
          {sending ? '发送中...' : '发送'}
        </button>
      </form>
    </section>
  )
}

package com.demo.booking.dto;

/**
 * 【传输对象】聊天请求体。
 * <p>
 * 前端 POST /api/chat 时发送的 JSON 格式。
 * </p>
 */
public class ChatRequest {

    /** 用户输入的自然语言，例如「我要订票」 */
    private String message;

    public ChatRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

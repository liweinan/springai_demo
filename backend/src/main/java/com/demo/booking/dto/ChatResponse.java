package com.demo.booking.dto;

/**
 * 【传输对象】聊天响应体。
 * <p>
 * 正常时只有 {@code reply}；调用 DeepSeek 失败时额外带 {@code error} 字段。
 * </p>
 */
public class ChatResponse {

    /** AI 最终回复文本 */
    private String reply;

    /** 错误信息，成功时为 null */
    private String error;

    public ChatResponse() {
    }

    public ChatResponse(String reply, String error) {
        this.reply = reply;
        this.error = error;
    }

    /** 成功响应 */
    public static ChatResponse success(String reply) {
        return new ChatResponse(reply, null);
    }

    /** 失败响应：仍返回 reply 便于前端展示，error 供调试 */
    public static ChatResponse failure(String reply, String error) {
        return new ChatResponse(reply, error);
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

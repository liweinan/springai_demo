package com.demo.booking.controller;

import com.demo.booking.dto.ChatRequest;
import com.demo.booking.dto.ChatResponse;
import com.demo.booking.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【表现层】聊天 REST API。
 * <p>
 * 只负责 HTTP 入参/出参，AI 逻辑全部委托给 {@link ChatService}。
 * </p>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 接收用户聊天消息，返回 AI 回复。
     * <p>
     * 请求示例：{@code POST /api/chat} body {@code {"message":"我要订票"}}
     * </p>
     *
     * @param request 包含 message 字段
     * @return reply 与可选的 error 字段
     */
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String message = request.getMessage() != null ? request.getMessage() : "";
        return chatService.chat(message);
    }
}

package com.demo.booking.controller;

import com.demo.booking.dto.HealthResponse;
import com.demo.booking.dto.LiveHealthResponse;
import com.demo.booking.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【表现层】健康检查 API。
 * <p>
 * <ul>
 *   <li>{@code GET /api/health/live} — 存活探针，不调用大模型（Docker HEALTHCHECK 用）</li>
 *   <li>{@code GET /api/health} — 完整检查，含 DeepSeek Key 配置与连通性（手动验收用）</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ChatService chatService;

    @Value("${spring.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    public HealthController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 轻量存活探针：只确认应用已启动，不访问 DeepSeek。
     */
    @GetMapping("/live")
    public LiveHealthResponse live() {
        return LiveHealthResponse.up();
    }

    /**
     * 完整健康检查：Key 是否配置 + DeepSeek 是否可达（会发送一次 {@code ping} 请求）。
     * <p>勿用于 Docker 高频 HEALTHCHECK，否则会持续消耗 API 配额。</p>
     */
    @GetMapping
    public HealthResponse health() {
        boolean configured = StringUtils.hasText(deepseekApiKey);
        boolean reachable = configured && chatService.pingDeepSeek();
        return new HealthResponse(configured, reachable);
    }
}

package com.demo.booking.controller;

import com.demo.booking.dto.HealthResponse;
import com.demo.booking.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【表现层】健康检查 API。
 * <p>
 * demo 启动后先 {@code curl http://localhost:8080/api/health}，
 * 确认 Key 已配置且 DeepSeek 可达，再测聊天。
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
     * 返回 DeepSeek 配置与连通性状态。
     *
     * @return deepseekConfigured / deepseekReachable 两个布尔字段
     */
    @GetMapping
    public HealthResponse health() {
        boolean configured = StringUtils.hasText(deepseekApiKey);
        boolean reachable = configured && chatService.pingDeepSeek();
        return new HealthResponse(configured, reachable);
    }
}

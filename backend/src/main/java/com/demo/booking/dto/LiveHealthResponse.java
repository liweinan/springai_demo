package com.demo.booking.dto;

/**
 * 【传输对象】轻量存活探针响应。
 * <p>
 * {@code GET /api/health/live} 仅表示 Spring Boot 进程已就绪，不调用 DeepSeek。
 * 供 Docker HEALTHCHECK 等高频探活使用，避免反复消耗 API 配额。
 * </p>
 */
public class LiveHealthResponse {

    private String status;

    public LiveHealthResponse() {
    }

    public LiveHealthResponse(String status) {
        this.status = status;
    }

    public static LiveHealthResponse up() {
        return new LiveHealthResponse("UP");
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

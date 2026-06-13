package com.demo.booking.dto;

/**
 * 【传输对象】健康检查响应。
 * <p>
 * 启动后可调用 {@code GET /api/health} 确认 DeepSeek 配置与连通性。
 * </p>
 */
public class HealthResponse {

    /** 环境变量 DEEPSEEK_API_KEY 是否已配置（非空） */
    private boolean deepseekConfigured;

    /** 是否成功连通 DeepSeek API */
    private boolean deepseekReachable;

    public HealthResponse() {
    }

    public HealthResponse(boolean deepseekConfigured, boolean deepseekReachable) {
        this.deepseekConfigured = deepseekConfigured;
        this.deepseekReachable = deepseekReachable;
    }

    public boolean isDeepseekConfigured() {
        return deepseekConfigured;
    }

    public void setDeepseekConfigured(boolean deepseekConfigured) {
        this.deepseekConfigured = deepseekConfigured;
    }

    public boolean isDeepseekReachable() {
        return deepseekReachable;
    }

    public void setDeepseekReachable(boolean deepseekReachable) {
        this.deepseekReachable = deepseekReachable;
    }
}

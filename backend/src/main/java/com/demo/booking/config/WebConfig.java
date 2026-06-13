package com.demo.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 相关配置。
 * <p>
 * 【为什么需要 CORS】
 * 前端 Vite 运行在 {@code http://localhost:5173}，后端在 {@code http://localhost:8080}，
 * 浏览器会认为这是「跨域」请求。若不配置 CORS，前端 fetch 会被浏览器拦截。
 * </p>
 * <p>
 * 开发阶段也可只用 Vite proxy 转发 /api，但加上 CORS 后 curl 和直连后端也更方便。
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}

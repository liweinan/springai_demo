package com.demo.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动入口。
 * <p>
 * 扫描 {@code com.demo.booking} 包下的所有组件（Controller、Service、Repository 等），
 * 并自动配置 Web 服务器、JPA、Spring AI 等。
 * </p>
 */
@SpringBootApplication
public class BookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}

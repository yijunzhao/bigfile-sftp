package com.rongzer.connector.bigfilesftp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/**
 * Web MVC 配置。
 *
 * <p>当前主要用于允许前端开发服务访问后端接口。</p>
 */
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册接口跨域规则。
     *
     * @param registry 跨域映射注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}

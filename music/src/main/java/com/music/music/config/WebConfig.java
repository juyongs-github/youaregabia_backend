package com.music.music.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer{
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "DELETE","PATCH", "OPTIONS")
            .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 기본 리소스 이미지
        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/images/");
        // 업로드 파일 처리 핸들러 (임시로 로컬)
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:./uploads/");
    }
}

package com.music.music.ratelimit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {

    
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(rateLimitInterceptor)
        // 감시할 경로를 컨트롤러 RequestMapping에 맞춰 정확히 등록
                .addPathPatterns(
                    "/community/share/**", 
                    "/boards/**", 
                    "/replies/**"
                )
                // 알림 조회 같은 GET 요청은 제외하고 싶다면 추가
                .excludePathPatterns("/api/notifications");
    }
}

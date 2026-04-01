package com.music.music.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Bean
    ModelMapper getMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(AccessLevel.PRIVATE)
        .setMatchingStrategy(MatchingStrategies.LOOSE);
        return modelMapper;
    }

    /** iTunes 곡 조회 결과 캐시 — 1시간 유지, 최대 2000곡 */
    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("itunesSongs");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(2000));
        return manager;
    }

    /** 추천 작업 전용 스레드풀 — DB 커넥션 풀(30) 초과 방지 */
    @Bean(name = "recommendationExecutor")
    Executor recommendationExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}

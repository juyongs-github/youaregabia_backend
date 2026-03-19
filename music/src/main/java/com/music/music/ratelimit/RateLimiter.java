package com.music.music.ratelimit;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class RateLimiter {
      // key: "userId:reply:create" → (횟수, 윈도우 만료시각)
    private final ConcurrentHashMap<String, long[]> store = new ConcurrentHashMap<>();

    /**
     return true  → 허용
     false → 초과
     */
    public boolean isAllowed(String email, RateLimitType type) {
        String key = email + ":" + type.getKey();
        long now = System.currentTimeMillis();
        long windowMs = type.getWindowSeconds() * 1000L;

        store.compute(key, (k, v) -> {
            // 첫 요청이거나 윈도우가 만료됐으면 초기화
            if (v == null || now > v[1]) {
                return new long[]{1, now + windowMs};
            }
            v[0]++;   // 카운트 증가
            return v;
        });
        // long[]의 구조: [0] = 현재 카운트, [1] = 윈도우 만료 시각(ms)
        long[] entry = store.get(key);
        return entry[0] <= type.getMaxCount();
    }
}

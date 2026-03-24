package com.music.music.ratelimit;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.music.music.user.entity.PointType;

@Component
public class RateLimiter {
    // key: "userId:reply:create" → (횟수, 윈도우 만료시각)
    private final ConcurrentHashMap<String, long[]> store = new ConcurrentHashMap<>();
    // 포인트 decay용 store — key: "email:POINT_TYPE", value: [count, windowExpireMs]
    private final ConcurrentHashMap<String, long[]> pointStore = new ConcurrentHashMap<>();

    private static final long POINT_WINDOW_MS = 60 * 60 * 1000L; // 1시간

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
    // 남은 시간(초) 반환
    public long getRemainSeconds(String email, RateLimitType type) {
        String key = email + ":" + type.getKey();
        long[] entry = store.get(key);
        if (entry == null) return 0;
        long remainMs = entry[1] - System.currentTimeMillis();
        return Math.max(0, remainMs / 1000);
    }

      // ===== 신규: 포인트 배율 반환 =====

    /**
     * 1시간 내 같은 PointType 적립 횟수에 따라 배율 반환
     * 1~3회  → 1.0 (100%)
     * 4~6회  → 0.5 (50%)
     * 7~9회  → 0.25 (25%)
     * 10회~  → 0.0 (0%)
     */
    public double getPointMultiplier(String email, PointType pointType) {
        String key = email + ":point:" + pointType.name();
        long now = System.currentTimeMillis();

        store.compute(key, (k, v) -> {    // pointStore 대신 store 통일해서 사용
            if (v == null || now > v[1]) {
                return new long[]{1, now + POINT_WINDOW_MS};
            }
            v[0]++;
            return v;
        });

        long count = store.get(key)[0];
        System.out.println(">>> PointDecay key: " + key + " | count: " + count);

        if (count <= 3)  return 1.0;
        if (count <= 6)  return 0.5;
        if (count <= 9)  return 0.25;
        return 0.0;
    }
}

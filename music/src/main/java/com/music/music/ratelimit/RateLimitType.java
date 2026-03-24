package com.music.music.ratelimit;

import lombok.Getter;

@Getter
public enum RateLimitType {
    BOARD_CREATE("board:create", 3, 10 * 60),
    REPLY_CREATE("reply:create", 3, 60),
    LIKE        ("like",         10, 60);

    private final String key;
    private final int maxCount;
    private final int windowSeconds;

    RateLimitType(String key, int maxCount, int windowSeconds) {
        this.key = key;
        this.maxCount = maxCount;
        this.windowSeconds = windowSeconds;
    }
}

package com.music.music.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RateLimitType {
    BOARD_CREATE("board:create", 3, 10 * 60),   // 10분에 3개
    REPLY_CREATE("reply:create", 3, 60),         // 1분에 3개
    LIKE        ("like",         10, 60);        // 1분에 10개

    private final String key;
    private final int maxCount;
    private final int windowSeconds;
}

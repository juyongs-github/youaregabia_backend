package com.music.music.exception;

import com.music.music.ratelimit.RateLimitType;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(RateLimitType type) {
        super(String.format("요청이 너무 많습니다. %d초 후 다시 시도해주세요.", type.getWindowSeconds()));
    }
}

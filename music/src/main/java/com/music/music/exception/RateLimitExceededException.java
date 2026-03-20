package com.music.music.exception;

import com.music.music.ratelimit.RateLimitType;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long remainSeconds;

    public RateLimitExceededException(RateLimitType type, long remainSeconds) {
        super(String.format("요청이 너무 많습니다. %d초 후 다시 시도해주세요.", remainSeconds));
        this.remainSeconds = remainSeconds;
    }
}

package com.music.music.auth.dto;

public record CiVerifyResponse(
    boolean success,
    String ci,
    boolean exists,
    String message) {
}

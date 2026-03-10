package com.music.music.auth.dto;

public record SocialRegisterRequest(
    String token,
    String name,
    String birthDate,
    String phoneNumber,
    String ci) {}

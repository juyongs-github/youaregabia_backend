package com.music.music.auth.oauth2;

public record OAuth2UserInfo(
    String provider,
    String providerId,
    String name,
    String email) {}

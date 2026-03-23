package com.music.music.api.service;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Service
public class SpotifyTokenService {

    private final Logger logger = LoggerFactory.getLogger(SpotifyTokenService.class);
    private final Gson gson = new Gson();

    @Value("${api.spotify.client-id}")
    private String clientId;

    @Value("${api.spotify.client-secret}")
    private String clientSecret;

    @Value("${api.spotify.redirect-uri}")
    private String redirectUri;

    // ✅ 토큰 캐싱
    private String cachedAccessToken;
    private String cachedRefreshToken;
    private long tokenExpiresAt = 0;

    private final RestClient authClient = RestClient.builder()
            .baseUrl("https://accounts.spotify.com")
            .build();

    // Access Token 반환 (만료 시 자동 갱신)
    public String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedAccessToken;
        }
        // refresh token 있으면 갱신
        if (cachedRefreshToken != null) {
            return refreshAccessToken();
        }
        logger.warn("[getAccessToken] 아직 인증이 완료되지 않았습니다. /spotify/login 먼저 접속하세요.");
        return null;
    }

    // ✅ 1단계: Authorization Code → Access Token + Refresh Token 교환
    public void exchangeCodeForToken(String code) {
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes());

            String body = authClient.post()
                    .uri("/api/token")
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=authorization_code"
                            + "&code=" + code
                            + "&redirect_uri=" + redirectUri)
                    .retrieve()
                    .body(String.class);

            SpotifyTokenResponse tokenResponse = gson.fromJson(body, SpotifyTokenResponse.class);
            saveToken(tokenResponse);
            logger.info("[exchangeCodeForToken] 토큰 발급 성공");
        } catch (Exception e) {
            logger.error("[exchangeCodeForToken] 토큰 발급 실패: {}", e.getMessage());
        }
    }

    // ✅ 2단계: Refresh Token으로 Access Token 갱신
    private String refreshAccessToken() {
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes());

            String body = authClient.post()
                    .uri("/api/token")
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=refresh_token&refresh_token=" + cachedRefreshToken)
                    .retrieve()
                    .body(String.class);

            SpotifyTokenResponse tokenResponse = gson.fromJson(body, SpotifyTokenResponse.class);
            saveToken(tokenResponse);
            logger.info("[refreshAccessToken] 토큰 갱신 성공");
            return cachedAccessToken;
        } catch (Exception e) {
            logger.error("[refreshAccessToken] 토큰 갱신 실패: {}", e.getMessage());
            return null;
        }
    }

    private void saveToken(SpotifyTokenResponse tokenResponse) {
        this.cachedAccessToken = tokenResponse.getAccessToken();
        this.tokenExpiresAt = System.currentTimeMillis()
                + (tokenResponse.getExpiresIn() - 60) * 1000L;
        // refresh_token은 최초 발급 시에만 오고 갱신 시엔 안 올 수 있음
        if (tokenResponse.getRefreshToken() != null) {
            this.cachedRefreshToken = tokenResponse.getRefreshToken();
        }
    }

    // 인증 완료 여부 확인
    public boolean isAuthenticated() {
        return cachedRefreshToken != null;
    }

    @Data
    private static class SpotifyTokenResponse {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("refresh_token")
        private String refreshToken;

        @SerializedName("expires_in")
        private int expiresIn;

        @SerializedName("token_type")
        private String tokenType;
    }
}
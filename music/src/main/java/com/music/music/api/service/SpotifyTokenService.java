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

    @Value("${api.spotify.client-id}")
    private String clientId;

    @Value("${api.spotify.client-secret}")
    private String clientSecret;

    private String cachedAccessToken;
    private long tokenExpiresAt = 0;

    private final Gson gson = new Gson();

    private final RestClient authClient = RestClient.builder()
            .baseUrl("https://accounts.spotify.com")
            .build();

    // Access Token 반환 (만료 시 자동 갱신)
    public String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedAccessToken;
        }
        return fetchNewToken();
    }

    // Client Credentials로 토큰 발급
    private String fetchNewToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        String body = authClient.post()
                .uri("/api/token")
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(String.class);

        SpotifyTokenResponse tokenResponse = gson.fromJson(body, SpotifyTokenResponse.class);
        this.cachedAccessToken = tokenResponse.getAccessToken();
        logger.info("[fetchNewToken] 토큰 발급 성공: {}", cachedAccessToken != null ? "OK" : "NULL");
        this.tokenExpiresAt = System.currentTimeMillis()
                + (tokenResponse.getExpiresIn() - 60) * 1000L;

        return cachedAccessToken;
    }

    // 토큰 응답 내부 클래스
    @Data
    private static class SpotifyTokenResponse {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("expires_in")
        private int expiresIn;

        @SerializedName("token_type")
        private String tokenType;
    }
}
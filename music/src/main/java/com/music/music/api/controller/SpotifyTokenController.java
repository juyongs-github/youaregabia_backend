package com.music.music.api.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.api.dto.SpotifySearchResponse;
import com.music.music.api.service.MusicApiService;
import com.music.music.api.service.SpotifyTokenService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/spotify")
public class SpotifyTokenController {

    private final Logger logger = LoggerFactory.getLogger(SpotifyTokenController.class);

    // ✅ 메모리 캐시 — 같은 곡은 Spotify API 재호출 없음
    private final Map<String, String> trackIdCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private SpotifyTokenService spotifyTokenService;

    @Autowired
    private MusicApiService musicApiService;

    @Value("${api.spotify.client-id}")
    private String clientId;

    @Value("${api.spotify.redirect-uri}")
    private String redirectUri;

    // 1단계: Spotify 로그인 페이지로 리다이렉트
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        String scope = "streaming%20user-read-email%20user-read-private%20user-modify-playback-state";
        String authUrl = "https://accounts.spotify.com/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&scope=" + scope
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        response.sendRedirect(authUrl);
    }

    // 2단계: 콜백 — code를 받아서 토큰으로 교환
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        spotifyTokenService.exchangeCodeForToken(code);
        return ResponseEntity.ok("Spotify 인증 완료! 이제 전곡 재생이 가능합니다.");
    }

    // Access Token 반환
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getToken() {
        try {
            String accessToken = spotifyTokenService.getAccessToken();
            if (accessToken == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "인증 필요. /spotify/login 먼저 접속하세요."));
            }
            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ Spotify Track ID 조회 — 캐시 우선, 없으면 API 호출 후 캐시 저장
    @GetMapping("/track-id")
    public ResponseEntity<Map<String, String>> getSpotifyTrackId(
            @RequestParam String trackName,
            @RequestParam String artistName) {
        try {
            String cacheKey = trackName + ":" + artistName;

            // 캐시 히트
            if (trackIdCache.containsKey(cacheKey)) {
                logger.info("[getSpotifyTrackId] 캐시 히트 - {}", trackName);
                return ResponseEntity.ok(Map.of("spotifyId", trackIdCache.get(cacheKey)));
            }

            // Spotify API 호출
            String query = trackName + " " + artistName;
            SpotifySearchResponse response = musicApiService.getTrackInfo(query, "track", 1);

            if (response != null
                    && response.getTracks() != null
                    && !response.getTracks().getItems().isEmpty()) {
                String spotifyId = response.getTracks().getItems().get(0).getId();

                // 캐시에 저장
                trackIdCache.put(cacheKey, spotifyId);

                logger.info("[getSpotifyTrackId] 조회 성공 - trackName: {}, spotifyId: {}", trackName, spotifyId);
                return ResponseEntity.ok(Map.of("spotifyId", spotifyId));
            }

            logger.warn("[getSpotifyTrackId] 조회 실패 - trackName: {}, artistName: {}", trackName, artistName);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("[getSpotifyTrackId] 에러 - {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
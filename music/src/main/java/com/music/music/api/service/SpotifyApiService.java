package com.music.music.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.music.music.api.entity.SpotifyTrackCache;
import com.music.music.api.repository.SpotifyTrackCacheRepository;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SpotifyApiService {
    private final Logger logger = LoggerFactory.getLogger(SpotifyApiService.class);

    @Value("${api.spotify.client-id}")
    private String clientId;

    @Value("${api.spotify.client-secret}")
    private String clientSecret;

    @Autowired
    private SpotifyTrackCacheRepository cacheRepository;

    // 토큰 인메모리 캐시 (서버 실행 중 1시간 유지)
    private String cachedToken = null;
    private long tokenExpiresAt = 0;

    // Spotify API 호출 간격 제한 (최소 1초)
    private final AtomicLong lastCallTime = new AtomicLong(0);

    private String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedToken;
        }

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
        formBody.add("grant_type", "client_credentials");

        String response = RestClient.builder()
                .baseUrl("https://accounts.spotify.com")
                .build()
                .post()
                .uri("/api/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body(String.class);

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        cachedToken = json.get("access_token").getAsString();
        tokenExpiresAt = System.currentTimeMillis() + 3540 * 1000L;
        return cachedToken;
    }

    public String searchTrackId(String trackName, String artistName) {
        // 1. DB 캐시 확인 (서버 재시작 후에도 유지)
        Optional<SpotifyTrackCache> cached = cacheRepository.findByTrackNameAndArtistName(trackName, artistName);
        if (cached.isPresent()) {
            return cached.get().getSpotifyTrackId();
        }

        // 2. 요청 간격 제한 (최소 1초)
        try {
            long now = System.currentTimeMillis();
            long wait = 1000L - (now - lastCallTime.get());
            if (wait > 0) Thread.sleep(wait);
            lastCallTime.set(System.currentTimeMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        // 3. Spotify API 호출
        try {
            String token = getAccessToken();
            String query = "track:" + trackName + " artist:" + artistName;

            String response = RestClient.builder()
                    .baseUrl("https://api.spotify.com")
                    .build()
                    .get()
                    .uri(url -> url.path("/v1/search")
                            .queryParam("q", query)
                            .queryParam("type", "track")
                            .queryParam("limit", 1)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonArray items = json.getAsJsonObject("tracks").getAsJsonArray("items");

            String trackId = items.size() > 0
                    ? items.get(0).getAsJsonObject().get("id").getAsString()
                    : null;

            // 검색 완료 시 DB 저장 (못 찾은 경우도 null로 저장해 재요청 방지)
            cacheRepository.save(new SpotifyTrackCache(trackName, artistName, trackId));
            return trackId;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                // 429는 캐시 저장 안 함 → 다음 요청 때 재시도 가능
                logger.warn("[searchTrackId] Spotify rate limit - {}, 잠시 후 재시도 가능", trackName);
            } else {
                logger.error("[searchTrackId] Spotify 트랙 검색 실패 - {}: {}", trackName, e.getMessage());
            }
            return null;
        } catch (Exception e) {
            logger.error("[searchTrackId] Spotify 트랙 검색 실패 - {}: {}", trackName, e.getMessage());
            return null;
        }
    }
}

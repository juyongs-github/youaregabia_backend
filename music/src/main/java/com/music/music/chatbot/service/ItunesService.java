package com.music.music.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ItunesService {

    private static final Logger logger = LoggerFactory.getLogger(ItunesService.class);

    private final RestClient restClient = RestClient.create();

    // AI 응답에서 곡명 - 아티스트 패턴 추출
    // 지원: "곡명" - 아티스트, **곡명** - 아티스트, 〈곡명〉 - 아티스트, <곡명> - 아티스트
    private static final Pattern SONG_PATTERN = Pattern.compile(
            "(?:[\"「『〈<]|\\*{1,2})([^\"」』〉>\n*]{2,60})(?:[\"」』〉>]|\\*{1,2})\\s*[-–—]\\s*([^\n,(\\[]{2,40})"
    );

    /**
     * AI 응답 텍스트에서 [곡명, 아티스트] 쌍을 추출합니다.
     */
    public List<String[]> extractSongs(String aiResponse) {
        List<String[]> songs = new ArrayList<>();
        if (aiResponse == null || aiResponse.isBlank()) return songs;

        Matcher matcher = SONG_PATTERN.matcher(aiResponse);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            String artist = matcher.group(2).trim();
            if (title.length() >= 1 && artist.length() >= 1) {
                songs.add(new String[]{title, artist});
            }
        }
        return songs;
    }

    /**
     * iTunes Search API로 곡 존재 여부를 검증합니다.
     * API 실패 시 true 반환 (통과 처리) — 네트워크 오류로 추천을 막지 않음
     */
    public boolean songExists(String title, String artist) {
        try {
            String query = URLEncoder.encode(title + " " + artist, StandardCharsets.UTF_8);
            String url = "https://itunes.apple.com/search?term=" + query
                    + "&media=music&limit=5&country=KR";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return true;
            Object count = response.get("resultCount");
            boolean found = count instanceof Integer && (Integer) count > 0;
            logger.debug("[iTunes] \"{}\" - {} : {}", title, artist, found ? "존재" : "미발견");
            return found;
        } catch (Exception e) {
            logger.warn("[iTunes] 검증 실패 ({} - {}): {}", title, artist, e.getMessage());
            return true; // 네트워크 오류 시 통과
        }
    }

    /**
     * 여러 곡을 병렬로 검증하고, 검증률(0.0~1.0)을 반환합니다.
     * 타임아웃(2초) 초과 시 해당 곡은 통과 처리합니다.
     */
    public double verifyRate(List<String[]> songs) {
        if (songs.isEmpty()) return 1.0;

        List<CompletableFuture<Boolean>> futures = songs.stream()
                .map(s -> CompletableFuture.supplyAsync(() -> songExists(s[0], s[1])))
                .collect(Collectors.toList());

        long verified = futures.stream()
                .mapToLong(f -> {
                    try {
                        return f.get(2, TimeUnit.SECONDS) ? 1L : 0L;
                    } catch (Exception e) {
                        return 1L; // 타임아웃 → 통과
                    }
                })
                .sum();

        double rate = (double) verified / songs.size();
        logger.info("[iTunes] 검증 결과: {}/{} 곡 확인 ({}%)", verified, songs.size(),
                String.format("%.0f", rate * 100));
        return rate;
    }

    /**
     * AI 응답에서 곡명만 추출해 "곡명 - 아티스트" 형식의 문자열 목록으로 반환합니다.
     * 중복 방지 프롬프트에 사용합니다.
     */
    public List<String> extractSongTitles(String aiResponse) {
        return extractSongs(aiResponse).stream()
                .map(s -> "\"" + s[0] + "\" - " + s[1])
                .collect(Collectors.toList());
    }
}

package com.music.music.recommendation.service;

import com.music.music.recommendation.dto.RecommendedSongDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * ytmusicapi Python 서비스(FastAPI)를 호출하는 Spring Boot 클라이언트.
 * YouTube Music 라디오 기반으로 특정 곡과 함께 자주 들리는 관련곡을 가져온다.
 */
@Service
public class YtMusicService {

    private static final Logger logger = LoggerFactory.getLogger(YtMusicService.class);

    private static final String[] YTMUSIC_REASONS = {
        "함께 자주 들리는 곡이에요",
        "비슷한 청취자들이 즐겨 듣는 곡이에요",
        "같은 플레이리스트에 자주 담기는 곡이에요",
        "이 곡 다음으로 듣기 좋은 곡이에요",
        "YouTube Music 라디오에서 연속으로 나오는 곡이에요"
    };
    private static final Random RANDOM = new Random();

    @Value("${ytmusic.service.url}")
    private String ytMusicServiceUrl;

    private final RestClient restClient = RestClient.create();
    private final ItunesSongFetcher itunesSongFetcher;

    public YtMusicService(ItunesSongFetcher itunesSongFetcher) {
        this.itunesSongFetcher = itunesSongFetcher;
    }

    /**
     * 특정 곡과 관련된 곡 목록을 YouTube Music에서 가져와 iTunes로 검증 후 반환.
     */
    @SuppressWarnings("unchecked")
    public List<RecommendedSongDto> getRelatedSongs(String trackName, String artistName, int limit) {
        try {
            String uri = UriComponentsBuilder.fromUriString(ytMusicServiceUrl + "/related")
                    .queryParam("title", trackName)
                    .queryParam("artist", artistName)
                    .queryParam("limit", limit)
                    .toUriString();

            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return Collections.emptyList();

            List<Map<String, String>> songs = (List<Map<String, String>>) response.get("songs");
            if (songs == null || songs.isEmpty()) return Collections.emptyList();

            // iTunes 조회를 병렬로 실행해 타임아웃 방지
            return songs.parallelStream()
                    .map(s -> {
                        String title = s.get("title");
                        String artist = s.get("artist");
                        return itunesSongFetcher.fetchAndSave(title, artist)
                                .map(dto -> RecommendedSongDto.builder()
                                        .song(dto)
                                        .reason(YTMUSIC_REASONS[RANDOM.nextInt(YTMUSIC_REASONS.length)])
                                        .source("ytmusic")
                                        .score(0.75)
                                        .build())
                                .orElse(null);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.warn("[YtMusicService] 관련곡 조회 실패 - {} {}: {}", trackName, artistName, e.getMessage());
            return Collections.emptyList();
        }
    }
}

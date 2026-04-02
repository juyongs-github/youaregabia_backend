package com.music.music.recommendation.service;

import com.music.music.playlist.entity.Song;
import com.music.music.playlist.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FAISS 벡터 인덱싱 배치 스케줄러.
 *
 * 매일 새벽 4시에 실행:
 *   1. Python 서버에서 이미 인덱싱된 곡 ID 목록 조회
 *   2. DB 전체 곡과 비교해 미인덱싱 곡만 추가 인덱싱
 *
 * → DB에 새 곡이 추가되어도 다음 날 자동으로 벡터 인덱스에 반영됨
 */
@Component
public class VectorIndexScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VectorIndexScheduler.class);

    @Value("${vector.service.url}")
    private String pythonServiceUrl;

    private final SongRepository songRepository;
    private final VectorSearchService vectorSearchService;

    public VectorIndexScheduler(SongRepository songRepository,
                                VectorSearchService vectorSearchService) {
        this.songRepository = songRepository;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * 매일 새벽 4시 증분 인덱싱.
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void indexNewSongs() {
        logger.info("[VectorIndexScheduler] 증분 인덱싱 배치 시작");
        try {
            Set<Long> indexedTextIds  = fetchIndexedIds("text_ids");
            Set<Long> indexedAudioIds = fetchIndexedIds("audio_ids");

            List<Song> allSongs = songRepository.findAll();

            // 텍스트 미인덱싱 곡
            List<Song> newTextSongs = allSongs.stream()
                    .filter(s -> !indexedTextIds.contains(s.getId()))
                    .collect(Collectors.toList());

            // 오디오 미인덱싱 곡 (previewUrl 있는 곡만)
            List<Song> newAudioSongs = allSongs.stream()
                    .filter(s -> !indexedAudioIds.contains(s.getId())
                            && s.getPreviewUrl() != null && !s.getPreviewUrl().isBlank())
                    .collect(Collectors.toList());

            logger.info("[VectorIndexScheduler] 텍스트 미인덱싱: {}곡 / 오디오 미인덱싱: {}곡",
                    newTextSongs.size(), newAudioSongs.size());

            if (newTextSongs.isEmpty() && newAudioSongs.isEmpty()) {
                logger.info("[VectorIndexScheduler] 신규 인덱싱 대상 없음 — 배치 종료");
                return;
            }

            // 텍스트 + 오디오 함께 인덱싱 (오디오 미인덱싱 곡 우선, 텍스트 전용 곡 후순위)
            Set<Long> audioTargetIds = newAudioSongs.stream()
                    .map(Song::getId).collect(Collectors.toSet());

            // 오디오 대상 곡은 previewUrl 포함 인덱싱
            vectorSearchService.indexSongs(newAudioSongs);

            // 텍스트만 필요한 곡 (오디오 대상에 없는 텍스트 미인덱싱 곡)
            List<Song> textOnlySongs = newTextSongs.stream()
                    .filter(s -> !audioTargetIds.contains(s.getId()))
                    .collect(Collectors.toList());
            vectorSearchService.indexSongs(textOnlySongs);

            logger.info("[VectorIndexScheduler] 배치 완료 — 텍스트 {}곡 / 오디오 {}곡 인덱싱",
                    newTextSongs.size(), newAudioSongs.size());

        } catch (Exception e) {
            logger.error("[VectorIndexScheduler] 배치 실패: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Long> fetchIndexedIds(String field) {
        try {
            RestClient client = RestClient.builder().baseUrl(pythonServiceUrl).build();
            Map<String, Object> response = client.get()
                    .uri("/vector/indexed-ids")
                    .retrieve()
                    .body(Map.class);
            if (response == null) return Set.of();
            List<Number> ids = (List<Number>) response.get(field);
            if (ids == null) return Set.of();
            return ids.stream().map(Number::longValue).collect(Collectors.toSet());
        } catch (Exception e) {
            logger.warn("[VectorIndexScheduler] 인덱싱 ID 조회 실패: {}", e.getMessage());
            return Set.of();
        }
    }
}

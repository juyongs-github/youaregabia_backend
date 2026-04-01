package com.music.music.recommendation.service;

import com.music.music.playlist.dto.SongDTO;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.repository.SongRepository;
import com.music.music.recommendation.dto.RecommendedSongDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * FAISS 기반 벡터 유사도 검색 서비스.
 * 벡터 저장·검색은 Python FastAPI(/vector/*) 에 위임하고,
 * 임베딩 생성만 Spring AI EmbeddingModel로 수행한다.
 */
@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    @Value("${ytmusic.service.url}")
    private String pythonServiceUrl;

    private RestClient pythonClient;
    private boolean vectorAvailable = false;

    private final EmbeddingModel embeddingModel;
    private final SongRepository songRepository;

    public VectorSearchService(EmbeddingModel embeddingModel, SongRepository songRepository) {
        this.embeddingModel = embeddingModel;
        this.songRepository = songRepository;
    }

    @PostConstruct
    public void init() {
        pythonClient = RestClient.builder().baseUrl(pythonServiceUrl).build();
        checkAvailability();
    }

    @SuppressWarnings("unchecked")
    private void checkAvailability() {
        try {
            java.util.Map<String, Object> status = pythonClient.get().uri("/status")
                    .retrieve()
                    .body(java.util.Map.class);
            vectorAvailable = true;
            if (status != null) {
                boolean ytmusicAuth      = Boolean.TRUE.equals(status.get("ytmusic_auth"));
                boolean ytmusicAvailable = Boolean.TRUE.equals(status.get("ytmusic_available"));
                Object textVectors  = status.get("faiss_text_vectors");
                Object audioVectors = status.get("faiss_audio_vectors");
                logger.info("[Python 서버] 연결 완료 - {}", pythonServiceUrl);
                logger.info("[Python 서버] YTMusic 인증: {} / 사용가능: {}", ytmusicAuth ? "O" : "X", ytmusicAvailable ? "O" : "X");
                logger.info("[Python 서버] FAISS 벡터 - 텍스트: {}개 / 오디오: {}개", textVectors, audioVectors);
            }
        } catch (Exception e) {
            vectorAvailable = false;
            logger.warn("[Python 서버] 연결 불가 — 벡터/YTMusic 검색 비활성화 (Python 서버 확인 필요)");
        }
    }

    /** 곡 1개를 FAISS에 인덱싱 (텍스트 벡터 + previewUrl 있으면 오디오 특징도 함께) */
    public void indexSong(Song song) {
        if (!vectorAvailable) return;
        try {
            List<Float> vector = embed(buildText(song));
            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("id",         song.getId());
            body.put("trackName",  song.getTrackName() != null ? song.getTrackName() : "");
            body.put("artistName", song.getArtistName() != null ? song.getArtistName() : "");
            body.put("genreName",  song.getGenreName() != null ? song.getGenreName() : "");
            body.put("vector",     vector);
            body.put("previewUrl", song.getPreviewUrl() != null ? song.getPreviewUrl() : "");
            pythonClient.post()
                    .uri("/vector/index")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("[VectorSearchService] 인덱싱 실패 - {}: {}", song.getTrackName(), e.getMessage());
        }
    }

    /** 곡 목록 일괄 인덱싱 */
    public void indexSongs(List<Song> songs) {
        songs.forEach(this::indexSong);
    }

    /**
     * 코사인 유사도 기반 유사곡 검색.
     * seed 곡 자신은 결과에서 제외.
     */
    @SuppressWarnings("unchecked")
    public List<RecommendedSongDto> findSimilar(
            String trackName, String artistName, String genreName, int limit) {
        if (!vectorAvailable) return Collections.emptyList();
        try {
            String text = trackName + " " + artistName + " " + (genreName != null ? genreName : "");
            List<Float> queryVector = embed(text);

            // 쿼리 곡의 previewUrl 조회 (오디오 유사도 결합용)
            String previewUrl = songRepository
                    .findByTrackNameAndArtistNameLike(trackName, artistName)
                    .map(s -> s.getPreviewUrl() != null ? s.getPreviewUrl() : "")
                    .orElse("");

            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("vector", queryVector);
            body.put("limit", limit + 3);
            body.put("previewUrl", previewUrl);

            Map<String, Object> response = pythonClient.post()
                    .uri("/vector/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return Collections.emptyList();

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");
            if (results == null) return Collections.emptyList();

            return results.stream()
                    .filter(r -> {
                        String name = (String) r.get("trackName");
                        double score = r.get("score") != null
                                ? ((Number) r.get("score")).doubleValue() : 0.0;
                        return name != null && !name.equalsIgnoreCase(trackName) && score >= 0.40;
                    })
                    .map(r -> {
                        Object idObj = r.get("id");
                        if (idObj == null) return null;
                        Long songId = ((Number) idObj).longValue();
                        double score = r.get("score") != null
                                ? ((Number) r.get("score")).doubleValue() : 0.0;

                        return songRepository.findById(songId)
                                .map(s -> RecommendedSongDto.builder()
                                        .song(toDto(s))
                                        .reason(vectorReason(score))
                                        .source("vector")
                                        .score(score)
                                        .build())
                                .orElse(null);
                    })
                    .filter(Objects::nonNull)
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.warn("[VectorSearchService] 유사곡 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── private helpers ──────────────────────────────────

    private String vectorReason(double score) {
        if (score >= 0.57) return "거의 같은 감성의 곡이에요";       // 텍스트 0.95 × 0.6
        if (score >= 0.51) return "분위기와 장르가 매우 비슷한 곡이에요"; // 텍스트 0.85 × 0.6
        if (score >= 0.45) return "비슷한 스타일의 곡이에요";         // 텍스트 0.75 × 0.6
        return "음악적 색깔이 유사한 곡이에요";
    }

    private List<Float> embed(String text) {
        float[] output = embeddingModel.embed(text);
        List<Float> result = new ArrayList<>(output.length);
        for (float v : output) result.add(v);
        return result;
    }

    private String buildText(Song song) {
        return song.getTrackName() + " " + song.getArtistName()
                + " " + (song.getGenreName() != null ? song.getGenreName() : "");
    }

    private SongDTO toDto(Song song) {
        return SongDTO.builder()
                .id(song.getId())
                .trackName(song.getTrackName())
                .artistName(song.getArtistName())
                .previewUrl(song.getPreviewUrl())
                .imgUrl(song.getImgUrl())
                .releaseDate(song.getReleaseDate())
                .durationMs(song.getDurationMs())
                .genreName(song.getGenreName())
                .build();
    }
}

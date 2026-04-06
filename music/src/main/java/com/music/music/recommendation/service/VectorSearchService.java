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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * FAISS 기반 벡터 유사도 검색 서비스.
 * 벡터 저장·검색은 Python FastAPI(/vector/*) 에 위임하고,
 * 임베딩 생성만 Spring AI EmbeddingModel로 수행한다.
 */
@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);
    private static final double MIN_SIMILARITY_SCORE_STRICT = 0.60;
    private static final double MIN_SIMILARITY_SCORE_RELAXED = 0.52;
    private static final int MIN_STRICT_RESULTS = 6;
    private static final long MIN_REASONABLE_DURATION_MS = 90_000L;
    private static final long MAX_REASONABLE_DURATION_MS = 720_000L;
    private static final long AVAILABILITY_RECHECK_INTERVAL_MS = 30_000L;
    private static final List<String> LOW_QUALITY_KEYWORDS = Arrays.asList(
            "inst", "instrumental", "mr", "노래방", "karaoke", "반주",
            "명상", "meditation", "healing", "힐링", "study", "공부", "asmr");

    @Value("${vector.service.url}")
    private String pythonServiceUrl;

    private RestClient pythonClient;
    private boolean vectorAvailable = false;
    private volatile long lastAvailabilityCheckAt = 0L;

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
        lastAvailabilityCheckAt = System.currentTimeMillis();
        try {
            java.util.Map<String, Object> status = pythonClient.get().uri("/status")
                    .retrieve()
                    .body(java.util.Map.class);
            vectorAvailable = true;
            if (status != null) {
                Object textVectors  = status.get("faiss_text_vectors");
                Object audioVectors = status.get("faiss_audio_vectors");
                logger.info("[Python 서버] 연결 완료 - {}", pythonServiceUrl);
                logger.info("[Python 서버] FAISS 벡터 - 텍스트: {}개 / 오디오: {}개", textVectors, audioVectors);
            }
        } catch (Exception e) {
            vectorAvailable = false;
            logger.warn("[Python 서버] 연결 불가 — 벡터 검색 비활성화 (Python 서버 확인 필요)");
        }
    }

    /** 곡 1개를 FAISS에 인덱싱 (텍스트 벡터 + previewUrl 있으면 오디오 특징도 함께) */
    private boolean ensureVectorAvailable() {
        long now = System.currentTimeMillis();
        if (!vectorAvailable && now - lastAvailabilityCheckAt >= AVAILABILITY_RECHECK_INTERVAL_MS) {
            checkAvailability();
        }
        return vectorAvailable;
    }

    public void indexSong(Song song) {
        if (!ensureVectorAvailable()) return;
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
        return findSimilarInternal(trackName, artistName, genreName, limit, true);
    }

    @SuppressWarnings("unchecked")
    private List<RecommendedSongDto> findSimilarInternal(
            String trackName, String artistName, String genreName, int limit, boolean allowAutoIndexOnEmpty) {
        if (!ensureVectorAvailable()) return Collections.emptyList();
        long startedAt = System.nanoTime();
        try {
            Song seedSong = songRepository
                    .findByTrackNameAndArtistNameLike(trackName, artistName)
                    .orElse(null);
            if (seedSong == null && trackName != null && !trackName.isBlank()) {
                seedSong = songRepository.findByTrackNameLike(trackName).orElse(null);
                if (seedSong != null) {
                    logger.info("[VectorSearchService] 기준곡 fallback 매칭 성공: \"{}\" - {}",
                            seedSong.getTrackName(), seedSong.getArtistName());
                }
            }
            String seedGenre = seedSong != null && seedSong.getGenreName() != null
                    ? seedSong.getGenreName() : genreName;
            Long seedDurationMs = seedSong != null ? seedSong.getDurationMs() : null;

            String text = buildSearchQuery(trackName, artistName, seedGenre, seedSong);
            long embedStartedAt = System.nanoTime();
            List<Float> queryVector = embed(text);
            long embedElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - embedStartedAt);

            // 쿼리 곡의 songId/previewUrl 조회 (오디오 유사도 결합용)
            String previewUrl = seedSong != null && seedSong.getPreviewUrl() != null
                    ? seedSong.getPreviewUrl() : "";

            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            int requestedLimit = Math.max(limit + 10, limit * 2);
            body.put("vector", queryVector);
            body.put("limit", requestedLimit);
            body.put("songId", seedSong != null ? seedSong.getId() : null);
            body.put("previewUrl", previewUrl);

            long pythonStartedAt = System.nanoTime();
            Map<String, Object> response = pythonClient.post()
                    .uri("/vector/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            long pythonElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - pythonStartedAt);

            if (response == null) return Collections.emptyList();

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.get("results");
            if (results == null) return Collections.emptyList();

            long mappingStartedAt = System.nanoTime();
            Map<Long, Song> songsById = loadSongsById(results);
            // 기준곡 ID (DB 기준) - 문자열 인코딩 차이에 관계없이 정확히 제외
            final Long seedSongId = seedSong != null ? seedSong.getId() : null;
            String seedTitleNorm = normalizeTitleOnly(trackName);
            String seedArtistNorm = normalizeArtistKey(artistName);

            List<VectorCandidate> allCandidates = results.stream()
                    .filter(r -> {
                        Object idObj = r.get("id");
                        String name = (String) r.get("trackName");
                        String artist = (String) r.get("artistName");
                        if (name == null) return false;
                        // DB ID 기준으로 기준곡 제외 (특수문자·인코딩 차이 무관)
                        if (seedSongId != null && idObj != null
                                && seedSongId.equals(((Number) idObj).longValue())) return false;
                        // 기준곡과 같은 아티스트의 다른 버전 제외 (Live, Remix 등)
                        if (!seedTitleNorm.isBlank()
                                && normalizeTitleOnly(name).equals(seedTitleNorm)
                                && normalizeArtistKey(artist).equals(seedArtistNorm)) return false;
                        return true;
                    })
                    .map(r -> {
                        Object idObj = r.get("id");
                        if (idObj == null) return null;
                        Long songId = ((Number) idObj).longValue();
                        double score = r.get("score") != null
                                ? ((Number) r.get("score")).doubleValue() : 0.0;
                        double textScore = r.get("text_score") != null
                                ? ((Number) r.get("text_score")).doubleValue() : score;
                        Double audioScore = r.get("audio_score") != null
                                ? ((Number) r.get("audio_score")).doubleValue() : null;
                        Map<String, Object> audioDetail = r.get("audio_detail") instanceof Map<?, ?>
                                ? (Map<String, Object>) r.get("audio_detail") : null;

                        Song song = songsById.get(songId);
                        if (song == null) return null;

                        return VectorCandidate.builder()
                                .song(song)
                                .baseScore(score)
                                .textScore(textScore)
                                .audioScore(audioScore)
                                .audioDetail(audioDetail)
                                .rerankedScore(rerankScore(
                                        score,
                                        textScore,
                                        audioScore,
                                        seedGenre,
                                        song.getGenreName(),
                                        seedDurationMs,
                                        song.getDurationMs(),
                                        song.getTrackName(),
                                        song.getArtistName()))
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .filter(c -> !isLowQualityCandidate(c.song.getTrackName(), c.song.getArtistName()))
                    .filter(c -> !isUnreasonableDuration(c.song.getDurationMs()))
                    .sorted((a, b) -> Double.compare(b.rerankedScore, a.rerankedScore))
                    .collect(Collectors.toList());

            // 60% 이상 후보 전체 수집
            List<VectorCandidate> eligible = allCandidates.stream()
                    .filter(c -> c.rerankedScore >= MIN_SIMILARITY_SCORE_STRICT)
                    .collect(Collectors.toList());

            // 조건 맞는 곡이 limit보다 많으면 랜덤하게 선택 (매 요청마다 다양한 곡 노출)
            if (eligible.size() > limit) {
                Collections.shuffle(eligible);
            }
            List<VectorCandidate> selected = eligible.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            List<RecommendedSongDto> recommendations = selected.stream()
                    .map(c -> RecommendedSongDto.builder()
                            .song(toDto(c.song))
                            .reason(vectorReason(c.rerankedScore, c.textScore, c.audioScore, c.audioDetail))
                            .source("vector")
                            .score(c.rerankedScore)
                            .build())
                    .collect(Collectors.toList());

            if (recommendations.isEmpty() && allowAutoIndexOnEmpty && seedSong != null) {
                logger.info("[VectorSearchService] 결과 0건 - 기준곡 자동 인덱싱 후 재시도: \"{}\" - {}",
                        seedSong.getTrackName(), seedSong.getArtistName());
                try {
                    indexSong(seedSong);
                } catch (Exception reindexEx) {
                    logger.warn("[VectorSearchService] 기준곡 자동 인덱싱 실패: {}", reindexEx.getMessage());
                }
                return findSimilarInternal(trackName, artistName, genreName, limit, false);
            }

            long mappingElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - mappingStartedAt);
            long totalElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            logger.info("[VectorSearchService] 호출 시간 - embed={}ms, python={}ms, mapping={}ms, total={}ms, 요청={}개, 후보={}곡, 결과={}곡",
                    embedElapsedMs, pythonElapsedMs, mappingElapsedMs, totalElapsedMs,
                    requestedLimit, allCandidates.size(), recommendations.size());
            return recommendations;

        } catch (Exception e) {
            logger.warn("[VectorSearchService] 유사곡 검색 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── private helpers ──────────────────────────────────

    private Map<Long, Song> loadSongsById(List<Map<String, Object>> results) {
        Set<Long> songIds = results.stream()
                .map(r -> r.get("id"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Song> songsById = new HashMap<>();
        songRepository.findAllById(songIds).forEach(song -> songsById.put(song.getId(), song));
        return songsById;
    }

    private String buildSearchQuery(String trackName, String artistName, String genreName, Song seedSong) {
        StringBuilder query = new StringBuilder();
        query.append(trackName != null ? trackName : "").append(" ")
                .append(artistName != null ? artistName : "").append(" ")
                .append(genreName != null ? genreName : "");
        if (seedSong != null && seedSong.getReleaseDate() != null) {
            String date = seedSong.getReleaseDate().toString();
            if (date.length() >= 4) {
                query.append(" ").append(date, 0, 4);
            }
        }
        return query.toString().trim();
    }

    private boolean isLowQualityCandidate(String trackName, String artistName) {
        String merged = ((trackName != null ? trackName : "") + " " + (artistName != null ? artistName : ""))
                .toLowerCase(Locale.ROOT);
        return LOW_QUALITY_KEYWORDS.stream().anyMatch(merged::contains);
    }

    private boolean isUnreasonableDuration(Long durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return false;
        }
        return durationMs < MIN_REASONABLE_DURATION_MS || durationMs > MAX_REASONABLE_DURATION_MS;
    }

    private double rerankScore(
            double baseScore,
            double textScore,
            Double audioScore,
            String seedGenre,
            String candidateGenre,
            Long seedDurationMs,
            Long candidateDurationMs,
            String trackName,
            String artistName) {
        double score = baseScore;

        // 오디오 유사도가 있으면 더 강하게 반영 (오디오 주도)
        if (audioScore != null && audioScore > 0) {
            score += (audioScore - textScore) * 0.30;
        }
        if (isGenreCompatible(seedGenre, candidateGenre)) {
            score += 0.03;
        } else {
            score -= 0.04;
        }
        if (isDurationCompatible(seedDurationMs, candidateDurationMs)) {
            score += 0.02;
        } else if (candidateDurationMs != null && candidateDurationMs > 0) {
            score -= 0.03;
        }
        if (isLowQualityCandidate(trackName, artistName)) {
            score -= 0.15;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    private boolean isDurationCompatible(Long seedDurationMs, Long candidateDurationMs) {
        if (seedDurationMs == null || candidateDurationMs == null || seedDurationMs <= 0 || candidateDurationMs <= 0) {
            return true;
        }
        long diff = Math.abs(seedDurationMs - candidateDurationMs);
        return diff <= 80_000L;
    }

    private boolean isGenreCompatible(String seedGenre, String candidateGenre) {
        if (seedGenre == null || seedGenre.isBlank() || candidateGenre == null || candidateGenre.isBlank()) {
            return true;
        }
        String s = normalizeGenre(seedGenre);
        String c = normalizeGenre(candidateGenre);
        if (s.equals(c) || s.contains(c) || c.contains(s)) {
            return true;
        }
        return sameGenreFamily(s, c);
    }

    /** 버전 표기 제거 후 제목 정규화 (기준곡 버전 필터링용) */
    private String normalizeTitleOnly(String title) {
        if (title == null) return "";
        return title
                .replaceAll("(?i)\\s*[-–]\\s*(live|acoustic|remix|inst\\.?|instrumental|radio\\s*edit|remaster(ed)?|version|ver\\.).*$", "")
                .replaceAll("(?i)\\s*\\(.*?(live|acoustic|remix|inst\\.?|instrumental|remaster(ed)?|version|ver\\.|반주|노래방|mr).*?\\)", "")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeArtistKey(String artist) {
        if (artist == null) return "";
        return artist.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeGenre(String genre) {
        return genre.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private boolean sameGenreFamily(String g1, String g2) {
        return inFamily(g1, g2, "kpop", "pop", "dance")
                || inFamily(g1, g2, "hiphop", "rap", "trap")
                || inFamily(g1, g2, "rnb", "soul")
                || inFamily(g1, g2, "ballad", "acoustic")
                || inFamily(g1, g2, "rock", "metal", "punk")
                || inFamily(g1, g2, "indie", "alternative")
                || inFamily(g1, g2, "electronic", "edm", "house", "techno")
                || inFamily(g1, g2, "jazz", "blues")
                || inFamily(g1, g2, "classical", "orchestra", "instrumental");
    }

    private boolean inFamily(String g1, String g2, String... keys) {
        boolean left = Arrays.stream(keys).anyMatch(g1::contains);
        boolean right = Arrays.stream(keys).anyMatch(g2::contains);
        return left && right;
    }

    private String vectorReason(double score, double textScore, Double audioScore, Map<String, Object> audioDetail) {
        String summary;
        if (score >= 0.57) summary = "지금 듣는 곡과 분위기가 정말 잘 이어져요.";
        else if (score >= 0.51) summary = "분위기와 결이 비슷해서 함께 듣기 좋아요.";
        else if (score >= 0.45) summary = "무드가 비슷해서 편하게 이어 들을 수 있어요.";
        else summary = "취향이 크게 벗어나지 않아 가볍게 들어보기 좋은 곡이에요.";

        if (audioScore != null && audioDetail != null && !audioDetail.isEmpty()) {
            return String.format(Locale.ROOT,
                    "%s %s %d%% 유사해요.",
                    summary,
                    audioReason(audioScore, audioDetail),
                    toSimilarityPercent(score));
        }

        if (audioScore != null && audioScore > 0.0) {
            return String.format(Locale.ROOT,
                    "%s %s %d%% 유사해요.",
                    summary,
                    genericAudioReason(audioScore),
                    toSimilarityPercent(score));
        }

        return String.format(Locale.ROOT,
                "%s %s %d%% 유사해요.",
                summary,
                textReason(textScore),
                toSimilarityPercent(score));
    }

    private int toSimilarityPercent(double score) {
        return (int) Math.round(score * 100);
    }

    private String audioReason(double audioScore, Map<String, Object> audioDetail) {
        Map<String, Double> featureScores = new LinkedHashMap<>();
        featureScores.put("템포가 비슷하고", getDouble(audioDetail, "tempo_similarity"));
        featureScores.put("에너지감이 잘 맞고", getDouble(audioDetail, "energy_similarity"));
        featureScores.put("음색 결이 닮아 있고", getDouble(audioDetail, "timbre_similarity"));
        featureScores.put("화성 느낌이 비슷하고", getDouble(audioDetail, "harmony_similarity"));
        featureScores.put("밝기와 공간감이 가깝고", getDouble(audioDetail, "brightness_similarity"));
        featureScores.put("리듬감이 잘 이어져서", getDouble(audioDetail, "rhythm_similarity"));

        List<String> topReasons = featureScores.entrySet().stream()
                .filter(e -> e.getValue() >= 0.55)
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topReasons.size() >= 2) {
            return topReasons.get(0) + " " + topReasons.get(1) + " 자연스럽게 이어져요.";
        }
        if (topReasons.size() == 1) {
            return topReasons.get(0) + " 전체적인 오디오 결도 잘 맞아요.";
        }
        if (audioScore >= 0.65) {
            return "오디오 분위기 전반이 꽤 잘 맞는 편이에요.";
        }
        return "오디오 분위기에서 닮은 지점이 보여요.";
    }

    private String genericAudioReason(double audioScore) {
        if (audioScore >= 0.80) {
            return "오디오 분위기와 결이 아주 비슷해요.";
        }
        if (audioScore >= 0.65) {
            return "오디오 느낌이 잘 이어지는 편이에요.";
        }
        if (audioScore >= 0.50) {
            return "오디오 분위기에서 비슷한 지점이 있어요.";
        }
        return "오디오 기준으로도 어느 정도 닮은 흐름이 있어요.";
    }

    private String textReason(double textScore) {
        if (textScore >= 0.75) {
            return "곡 정보 기준으로도 꽤 가깝게 잡혔어요.";
        }
        if (textScore >= 0.55) {
            return "곡명, 아티스트, 장르 흐름이 잘 맞는 편이에요.";
        }
        return "곡 정보 기준으로도 어느 정도 비슷한 흐름이 있어요.";
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
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

    private static class VectorCandidate {
        private Song song;
        private double baseScore;
        private double textScore;
        private Double audioScore;
        private Map<String, Object> audioDetail;
        private double rerankedScore;

        private static Builder builder() {
            return new Builder();
        }

        private static class Builder {
            private final VectorCandidate value = new VectorCandidate();

            private Builder song(Song song) {
                value.song = song;
                return this;
            }

            private Builder baseScore(double baseScore) {
                value.baseScore = baseScore;
                return this;
            }

            private Builder textScore(double textScore) {
                value.textScore = textScore;
                return this;
            }

            private Builder audioScore(Double audioScore) {
                value.audioScore = audioScore;
                return this;
            }

            private Builder audioDetail(Map<String, Object> audioDetail) {
                value.audioDetail = audioDetail;
                return this;
            }

            private Builder rerankedScore(double rerankedScore) {
                value.rerankedScore = rerankedScore;
                return this;
            }

            private VectorCandidate build() {
                return value;
            }
        }
    }
}

package com.music.music.recommendation.service;

import com.music.music.api.service.MusicApiService;
import com.music.music.playlist.dto.SongDTO;
import com.music.music.recommendation.dto.RecommendedSongDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeoutException;

/**
 * 하이브리드 음악 추천 오케스트레이터.
 *
 * 두 소스를 병렬 호출 후 결과를 병합한다:
 *   1. FAISS   — 벡터 유사도 기반 (장르·분위기 유사곡)
 *   2. Last.fm — 유사곡/유사아티스트 기반
 *
 * 각 소스의 실패는 다른 소스에 영향을 주지 않는다.
 */
@Service
public class RecommendationOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationOrchestrator.class);

    private static final int MAX_RECOMMEND_TIMEOUT_MS = 5000;
    /** 후보 pool 캐시 유지 시간 (비용이 큰 OpenAI 임베딩·API 호출 결과) */
    private static final long POOL_CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final int PER_SOURCE_LIMIT = 10;
    /** 매 요청마다 항상 고정으로 노출할 상위 곡 수 */
    private static final int PINNED_TOP_COUNT = 3;
    private static final String[] LASTFM_STABLE_REASONS = {
            "취향 흐름을 크게 벗어나지 않아 안정적으로 이어 듣기 좋아요.",
            "분위기 결이 비슷해서 다음 곡으로 붙였을 때 자연스러워요.",
            "같은 취향 축 안에서 무드가 잘 이어지는 곡이에요.",
            "지금 재생 중인 곡과 톤이 맞아서 부담 없이 이어져요."
    };

    private final MusicApiService musicApiService;
    private final VectorSearchService vectorSearchService;
    /** pool 캐시: 소스별 후보 전체를 점수 순으로 저장 */
    private final Map<String, CacheEntry> poolCache = new ConcurrentHashMap<>();

    public RecommendationOrchestrator(
            MusicApiService musicApiService,
            VectorSearchService vectorSearchService) {
        this.musicApiService = musicApiService;
        this.vectorSearchService = vectorSearchService;
    }

    public List<RecommendedSongDto> recommend(
            String trackName, String artistName, String genreName, int limit) {

        String cacheKey = buildCacheKey(trackName, artistName, genreName, limit);

        // pool 캐시 확인 (API 호출·임베딩 비용 절감)
        List<RecommendedSongDto> pool = getFreshPool(cacheKey);

        if (pool == null) {
            pool = buildPool(trackName, artistName, genreName, limit);
            if (!pool.isEmpty()) {
                rememberPool(cacheKey, pool);
            } else {
                // 실시간 결과 없으면 stale 캐시 폴백
                CacheEntry stale = poolCache.get(cacheKey);
                if (stale != null && !stale.pool.isEmpty()) {
                    logger.warn("[Orchestrator] 실시간 결과 없음 - 스테일 pool 폴백 사용");
                    pool = stale.pool;
                }
            }
        }

        // pool에서 매 요청마다 새로 선택: 상위 N곡 고정 + 나머지 랜덤
        List<RecommendedSongDto> result = selectFromPool(pool, limit);
        logSourceSongs("final", result);
        return result;
    }

    /** API·임베딩 호출로 후보 pool을 새로 빌드 (점수 순 정렬) */
    private List<RecommendedSongDto> buildPool(
            String trackName, String artistName, String genreName, int limit) {

        int candidateLimit = Math.max(limit + 2, 8);
        long startedAt = System.nanoTime();

        CompletableFuture<List<RecommendedSongDto>> lastFmFuture =
                CompletableFuture.supplyAsync(() -> getLastFmRecommendations(trackName, artistName));
        CompletableFuture<List<RecommendedSongDto>> vectorFuture =
                CompletableFuture.supplyAsync(() ->
                        vectorSearchService.findSimilar(trackName, artistName, genreName, candidateLimit));

        boolean timedOut = false;
        try {
            CompletableFuture.allOf(lastFmFuture, vectorFuture)
                    .get(MAX_RECOMMEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            timedOut = true;
            logger.warn("[Orchestrator] {}ms 타임아웃 - 가용 결과로 진행", MAX_RECOMMEND_TIMEOUT_MS);
        } catch (Exception e) {
            logger.warn("[Orchestrator] 병렬 추천 처리 중 예외: {}", e.getMessage());
        }

        if (timedOut) {
            if (!lastFmFuture.isDone()) lastFmFuture.cancel(true);
            if (!vectorFuture.isDone()) vectorFuture.cancel(true);
        }

        List<RecommendedSongDto> lastFmResult = safeGet(lastFmFuture);
        List<RecommendedSongDto> vectorResult  = safeGet(vectorFuture);
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        logger.info("[Orchestrator] 소스별 후보: lastfm={}, vector={} ({}ms)",
                lastFmResult.size(), vectorResult.size(), elapsed);
        logSourceSongs("vector", vectorResult);
        logSourceSongs("lastfm", lastFmResult);

        List<RecommendedSongDto> merged = new ArrayList<>();
        merged.addAll(vectorResult);
        merged.addAll(lastFmResult);

        // ID 기준 중복 제거
        Map<Long, RecommendedSongDto> deduped = new LinkedHashMap<>();
        for (RecommendedSongDto dto : merged) {
            if (dto.getSong() != null && dto.getSong().getId() != null) {
                deduped.putIfAbsent(dto.getSong().getId(), dto);
            }
        }

        // 버전 표기 기준 중복 제거 (Live, Remix 등)
        Map<String, RecommendedSongDto> versionDeduped = new LinkedHashMap<>();
        for (RecommendedSongDto dto : deduped.values()) {
            String key = normalizeTrackKey(dto.getSong().getTrackName(), dto.getSong().getArtistName());
            versionDeduped.putIfAbsent(key, dto);
        }

        // 버전 트랙 제거 + 점수 내림차순 정렬 → pool 확정
        List<RecommendedSongDto> pool = versionDeduped.values().stream()
                .filter(dto -> !isVersionedTrack(dto.getSong().getTrackName()))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        // 아티스트별 최대 3곡 제한
        final int MAX_PER_ARTIST = 3;
        Map<String, Integer> artistCount = new LinkedHashMap<>();
        List<RecommendedSongDto> artistLimited = new ArrayList<>();
        for (RecommendedSongDto dto : pool) {
            List<String> keys = extractArtistKeys(dto.getSong().getArtistName());
            boolean anyAtLimit = keys.stream()
                    .anyMatch(k -> artistCount.getOrDefault(k, 0) >= MAX_PER_ARTIST);
            if (!anyAtLimit) {
                artistLimited.add(dto);
                keys.forEach(k -> artistCount.merge(k, 1, Integer::sum));
            }
        }
        logger.info("[Orchestrator] pool 빌드 완료: {}", artistLimited.size());
        return artistLimited;
    }

    /**
     * pool에서 매 요청마다 새로 선택.
     * 상위 PINNED_TOP_COUNT 곡은 항상 고정 노출, 나머지는 랜덤 선택.
     */
    private List<RecommendedSongDto> selectFromPool(List<RecommendedSongDto> pool, int limit) {
        if (pool.isEmpty()) return List.of();
        if (pool.size() <= limit) {
            // pool 전체 반환 (고정곡 이후는 가볍게 섞어서)
            List<RecommendedSongDto> result = new ArrayList<>(pool);
            if (result.size() > PINNED_TOP_COUNT) {
                Collections.shuffle(result.subList(PINNED_TOP_COUNT, result.size()));
            }
            return result;
        }

        // 상위 PINNED_TOP_COUNT는 항상 고정
        int pinned = Math.min(PINNED_TOP_COUNT, limit);
        List<RecommendedSongDto> result = new ArrayList<>(pool.subList(0, pinned));

        // 나머지 pool에서 랜덤 선택
        List<RecommendedSongDto> rest = new ArrayList<>(pool.subList(pinned, pool.size()));
        Collections.shuffle(rest);
        result.addAll(rest.subList(0, Math.min(limit - pinned, rest.size())));

        // 최종 점수 순 정렬
        result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return result;
    }

    private List<RecommendedSongDto> getLastFmRecommendations(String trackName, String artistName) {
        try {
            List<SongDTO> stableList = musicApiService.getRecommendSongList(trackName, artistName);
            if (stableList == null || stableList.isEmpty()) {
                return List.of();
            }

            AtomicInteger order = new AtomicInteger(0);
            return stableList.stream()
                    .limit(PER_SOURCE_LIMIT)
                    .map(song -> {
                        int idx = order.getAndIncrement();
                        double score = Math.max(0.60, 0.92 - (idx * 0.06));
                        return RecommendedSongDto.builder()
                                .song(song)
                                .reason(buildLastFmStableReason(trackName, artistName))
                                .source("lastfm")
                                .score(score)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("[RecommendationOrchestrator] Last.fm 실패: {}", e.getMessage());
        }
        return List.of();
    }


    private boolean isVersionedTrack(String trackName) {
        if (trackName == null) return false;
        String lower = trackName.toLowerCase();
        return lower.matches(".*\\b(live|acoustic|remix|instrumental|inst|remastered|remaster|radio\\s*edit|version|ver\\.)\\b.*")
                || lower.matches(".*[\\(\\[].*(live|acoustic|remix|instrumental|inst|remastered|반주|노래방|mr).*[\\)\\]].*");
    }

    private List<String> extractArtistKeys(String artistName) {
        if (artistName == null) return List.of("");
        return Arrays.stream(artistName.split("(?i)[&,×]|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bwith\\b"))
                .map(s -> s.trim().replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeTrackKey(String trackName, String artistName) {
        if (trackName == null) return "";
        String base = trackName
                .replaceAll("(?i)\\s*[-–]\\s*(live|acoustic|remix|inst\\.?|instrumental|radio edit|remaster(ed)?|version|ver\\.?).*$", "")
                .replaceAll("(?i)\\s*\\(.*?(live|acoustic|remix|inst\\.?|instrumental|remaster(ed)?|version|ver\\.|반주|노래방).*?\\)", "")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .toLowerCase();
        String artist = artistName != null ? artistName.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase() : "";
        return base + "_" + artist;
    }

    private List<RecommendedSongDto> safeGet(CompletableFuture<List<RecommendedSongDto>> future) {
        try {
            return future.isDone() ? future.get() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildCacheKey(String trackName, String artistName, String genreName, int limit) {
        String t = trackName == null ? "" : trackName.trim().toLowerCase(Locale.ROOT);
        String a = artistName == null ? "" : artistName.trim().toLowerCase(Locale.ROOT);
        String g = genreName == null ? "" : genreName.trim().toLowerCase(Locale.ROOT);
        return t + "|" + a + "|" + g + "|" + limit;
    }

    private List<RecommendedSongDto> getFreshPool(String cacheKey) {
        CacheEntry entry = poolCache.get(cacheKey);
        if (entry == null) return null;
        if (entry.ageMs() > POOL_CACHE_TTL_MS) {
            poolCache.remove(cacheKey);
            return null;
        }
        logger.info("[Orchestrator] pool 캐시 히트 (age={}ms) - 새로 랜덤 선택", entry.ageMs());
        return entry.pool;
    }

    private void rememberPool(String cacheKey, List<RecommendedSongDto> pool) {
        if (pool == null || pool.isEmpty()) return;
        poolCache.put(cacheKey, new CacheEntry(new ArrayList<>(pool)));
    }

    private List<RecommendedSongDto> trimToLimit(List<RecommendedSongDto> recommendations, int limit) {
        if (recommendations == null) return List.of();
        if (recommendations.size() <= limit) return recommendations;
        return new ArrayList<>(recommendations.subList(0, limit));
    }

    private String buildLastFmStableReason(String seedTrackName, String seedArtistName) {
        if (ThreadLocalRandom.current().nextInt(100) < 40) {
            return String.format("\"%s - %s\"와 흐름이 맞는 곡으로 골랐어요. %s",
                    seedTrackName, seedArtistName, pick(LASTFM_STABLE_REASONS));
        }
        return pick(LASTFM_STABLE_REASONS);
    }

    private String pick(String[] candidates) {
        return candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
    }

    private void logSourceSongs(String source, List<RecommendedSongDto> songs) {
        if (songs == null || songs.isEmpty()) {
            logger.info("[Orchestrator] {} 곡 목록: 없음", source);
            return;
        }
        Map<String, RecommendedSongDto> unique = new LinkedHashMap<>();
        for (RecommendedSongDto dto : songs) {
            SongDTO s = dto.getSong();
            if (s == null) continue;
            String key = normalizeTrackKey(s.getTrackName(), s.getArtistName());
            unique.merge(key, dto, (a, b) -> a.getScore() >= b.getScore() ? a : b);
        }
        String summary = unique.values().stream()
                .map(dto -> String.format(Locale.ROOT, "\"%s\" - %s (%.2f)",
                        dto.getSong().getTrackName(), dto.getSong().getArtistName(), dto.getScore()))
                .collect(Collectors.joining(", "));
        logger.info("[Orchestrator] {} 곡 목록: {}", source, summary);
    }

    public String toPromptContext(List<RecommendedSongDto> recommendations) {
        if (recommendations.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[유사곡 분석 결과]\n");
        for (RecommendedSongDto r : recommendations) {
            SongDTO s = r.getSong();
            sb.append(String.format("- \"%s\" - %s (%s)\n",
                    s.getTrackName(), s.getArtistName(), r.getReason()));
        }
        return sb.toString();
    }

    private static class CacheEntry {
        private final List<RecommendedSongDto> pool;
        private final long cachedAtMs;

        private CacheEntry(List<RecommendedSongDto> pool) {
            this.pool = pool;
            this.cachedAtMs = System.currentTimeMillis();
        }

        private long ageMs() {
            return System.currentTimeMillis() - cachedAtMs;
        }
    }
}

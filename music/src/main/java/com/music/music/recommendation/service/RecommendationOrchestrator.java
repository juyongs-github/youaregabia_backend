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
    private static final long RECOMMEND_CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final int PER_SOURCE_LIMIT = 10;
    private static final int PINNED_TOP_COUNT = 2;
    private static final String[] LASTFM_STABLE_REASONS = {
            "취향 흐름을 크게 벗어나지 않아 안정적으로 이어 듣기 좋아요.",
            "분위기 결이 비슷해서 다음 곡으로 붙였을 때 자연스러워요.",
            "같은 취향 축 안에서 무드가 잘 이어지는 곡이에요.",
            "지금 재생 중인 곡과 톤이 맞아서 부담 없이 이어져요."
    };

    private final MusicApiService musicApiService;
    private final VectorSearchService vectorSearchService;
    private final Map<String, CacheEntry> recommendationCache = new ConcurrentHashMap<>();

    public RecommendationOrchestrator(
            MusicApiService musicApiService,
            VectorSearchService vectorSearchService) {
        this.musicApiService = musicApiService;
        this.vectorSearchService = vectorSearchService;
    }

    public List<RecommendedSongDto> recommend(
            String trackName, String artistName, String genreName, int limit) {

        int candidateLimit = Math.max(limit + 2, 8);
        String cacheKey = buildCacheKey(trackName, artistName, genreName, limit);
        CacheEntry freshCache = getFreshCache(cacheKey);
        if (freshCache != null) {
            logger.info("[Orchestrator] 캐시 히트 - {}ms 내 반환", freshCache.ageMs());
            return trimToLimit(new ArrayList<>(freshCache.recommendations), limit);
        }

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
        } catch (TimeoutException timeoutException) {
            timedOut = true;
            logger.warn("[RecommendationOrchestrator] {}ms 타임아웃 - 가용 결과로 진행",
                    MAX_RECOMMEND_TIMEOUT_MS);
        } catch (Exception e) {
            logger.warn("[RecommendationOrchestrator] 병렬 추천 처리 중 예외: {}", e.getMessage());
        }

        if (timedOut) {
            if (!lastFmFuture.isDone()) lastFmFuture.cancel(true);
            if (!vectorFuture.isDone()) vectorFuture.cancel(true);
        }

        List<RecommendedSongDto> lastFmResult = safeGet(lastFmFuture);
        List<RecommendedSongDto> vectorResult = safeGet(vectorFuture);
        long totalElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        logger.info("[Orchestrator] 소스별 후보: lastfm={}, vector={} (total={}ms, timeout={}ms)",
                lastFmResult.size(), vectorResult.size(), totalElapsedMs, MAX_RECOMMEND_TIMEOUT_MS);
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

        // 버전 트랙 제거 + 점수 정렬
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
        pool = artistLimited;

        logger.info("[Orchestrator] 필터 후 pool={}", pool.size());

        if (pool.size() <= limit) {
            Collections.shuffle(pool);
            rememberCache(cacheKey, pool);
            return trimToLimit(pool, limit);
        }

        // vector : lastfm = 3:2 비율 배분
        List<RecommendedSongDto> vectorPool = new ArrayList<>();
        List<RecommendedSongDto> lastFmPool = new ArrayList<>();
        for (RecommendedSongDto dto : pool) {
            if ("lastfm".equals(dto.getSource())) lastFmPool.add(dto);
            else                                  vectorPool.add(dto);
        }

        int vectorTarget = Math.max(1, limit * 3 / 5);
        int lastFmTarget = limit - vectorTarget;
        int vectorSlots  = Math.min(vectorTarget, vectorPool.size());
        int lastFmSlots  = Math.min(lastFmTarget, lastFmPool.size());

        // 한 쪽이 부족하면 다른 쪽으로 보충
        int remaining = limit - (vectorSlots + lastFmSlots);
        if (remaining > 0 && vectorPool.size() > vectorSlots) {
            vectorSlots += Math.min(remaining, vectorPool.size() - vectorSlots);
        } else if (remaining > 0 && lastFmPool.size() > lastFmSlots) {
            lastFmSlots += Math.min(remaining, lastFmPool.size() - lastFmSlots);
        }

        logger.info("[Orchestrator] 슬롯 배분: vector={}/{}, lastfm={}/{}, 합계={}",
                vectorSlots, vectorTarget, lastFmSlots, lastFmTarget, vectorSlots + lastFmSlots);

        List<RecommendedSongDto> result = new ArrayList<>();
        result.addAll(pickWithVariety(vectorPool, vectorSlots));
        result.addAll(pickWithVariety(lastFmPool, lastFmSlots));

        Collections.shuffle(result);
        result = trimToLimit(result, limit);
        logSourceSongs("final", result);

        if (!result.isEmpty()) {
            rememberCache(cacheKey, result);
            return result;
        }

        CacheEntry staleCache = recommendationCache.get(cacheKey);
        if (staleCache != null && !staleCache.recommendations.isEmpty()) {
            logger.warn("[Orchestrator] 실시간 결과 비어 스테일 캐시 폴백 사용 (age={}ms)",
                    staleCache.ageMs());
            return trimToLimit(new ArrayList<>(staleCache.recommendations), limit);
        }
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
                        double score = Math.max(0.35, 0.92 - (idx * 0.06));
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

    private List<RecommendedSongDto> pickWithVariety(List<RecommendedSongDto> pool, int n) {
        if (n <= 0 || pool.isEmpty()) return Collections.emptyList();
        if (pool.size() <= n) return new ArrayList<>(pool);
        int pinned = Math.min(PINNED_TOP_COUNT, n);
        List<RecommendedSongDto> result = new ArrayList<>(pool.subList(0, pinned));
        List<RecommendedSongDto> rest = new ArrayList<>(pool.subList(pinned, pool.size()));
        Collections.shuffle(rest);
        result.addAll(rest.subList(0, Math.min(n - pinned, rest.size())));
        return result;
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

    private CacheEntry getFreshCache(String cacheKey) {
        CacheEntry entry = recommendationCache.get(cacheKey);
        if (entry == null) return null;
        if (entry.ageMs() > RECOMMEND_CACHE_TTL_MS) {
            recommendationCache.remove(cacheKey);
            return null;
        }
        return entry;
    }

    private void rememberCache(String cacheKey, List<RecommendedSongDto> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) return;
        recommendationCache.put(cacheKey, new CacheEntry(new ArrayList<>(recommendations)));
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
        private final List<RecommendedSongDto> recommendations;
        private final long cachedAtMs;

        private CacheEntry(List<RecommendedSongDto> recommendations) {
            this.recommendations = recommendations;
            this.cachedAtMs = System.currentTimeMillis();
        }

        private long ageMs() {
            return System.currentTimeMillis() - cachedAtMs;
        }
    }
}

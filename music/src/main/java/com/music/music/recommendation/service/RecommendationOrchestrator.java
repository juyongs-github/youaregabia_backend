package com.music.music.recommendation.service;

import com.music.music.api.dto.ArtistDTO;
import com.music.music.api.dto.TrackDTO;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 하이브리드 음악 추천 오케스트레이터.
 *
 * 세 소스를 병렬 호출 후 결과를 병합한다:
 *   1. Last.fm  — 유사곡/유사아티스트 기반 (기존 로직 재사용)
 *   2. Qdrant   — 벡터 유사도 기반 (장르·분위기 유사곡)
 *   3. ytmusic  — YouTube Music 라디오 기반 (함께 자주 들리는 곡)
 *
 * 각 소스의 실패는 다른 소스에 영향을 주지 않는다.
 */
@Service
public class RecommendationOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationOrchestrator.class);

    private static final int TIMEOUT_SECONDS = 6;
    private static final int YTMUSIC_TIMEOUT_SECONDS = 20;
    private static final int PER_SOURCE_LIMIT = 8;
    /** 후보 풀 배수 — 실제 limit보다 N배 많이 가져와서 랜덤 샘플링에 사용 */
    private static final int CANDIDATE_MULTIPLIER = 3;
    /** 매 추천에서 고정으로 포함할 최고 점수 곡 수 */
    private static final int PINNED_TOP_COUNT = 2;
    /** 최종 결과에서 YtMusic 소스로 보장할 최소 슬롯 수 */
    private static final int YTMUSIC_MIN_SLOTS = 3;

    private final MusicApiService musicApiService;
    private final VectorSearchService vectorSearchService;
    private final YtMusicService ytMusicService;
    private final ItunesSongFetcher itunesSongFetcher;

    public RecommendationOrchestrator(
            MusicApiService musicApiService,
            VectorSearchService vectorSearchService,
            YtMusicService ytMusicService,
            ItunesSongFetcher itunesSongFetcher) {
        this.musicApiService = musicApiService;
        this.vectorSearchService = vectorSearchService;
        this.ytMusicService = ytMusicService;
        this.itunesSongFetcher = itunesSongFetcher;
    }

    /**
     * 세 소스를 병렬 호출해 합산한 유사곡 목록을 반환한다.
     *
     * @param trackName  기준 곡명
     * @param artistName 기준 아티스트명
     * @param genreName  기준 장르 (벡터 검색에 사용, null 가능)
     * @param limit      최종 반환할 곡 수
     */
    public List<RecommendedSongDto> recommend(
            String trackName, String artistName, String genreName, int limit) {

        // 후보 풀 확대: 실제 필요한 수의 CANDIDATE_MULTIPLIER배 요청
        int candidateLimit = PER_SOURCE_LIMIT * CANDIDATE_MULTIPLIER;

        // Vector + YtMusic + Last.fm 병렬 호출
        CompletableFuture<List<RecommendedSongDto>> vectorFuture =
                CompletableFuture.supplyAsync(() ->
                        vectorSearchService.findSimilar(trackName, artistName, genreName, candidateLimit));

        CompletableFuture<List<RecommendedSongDto>> ytFuture =
                CompletableFuture.supplyAsync(() ->
                        ytMusicService.getRelatedSongs(trackName, artistName, candidateLimit));

        CompletableFuture<List<RecommendedSongDto>> lastFmFuture =
                CompletableFuture.supplyAsync(() ->
                        getLastFmRecommendations(trackName, artistName));

        try {
            CompletableFuture.allOf(vectorFuture, ytFuture, lastFmFuture)
                    .get(YTMUSIC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("[RecommendationOrchestrator] 타임아웃 - 가용 결과로 진행");
        }

        List<RecommendedSongDto> merged = new ArrayList<>();
        merged.addAll(safeGet(vectorFuture));
        merged.addAll(safeGet(ytFuture));
        merged.addAll(safeGet(lastFmFuture));

        // 곡 ID 기준 중복 제거 (앞쪽 소스 우선)
        Map<Long, RecommendedSongDto> deduped = new LinkedHashMap<>();
        for (RecommendedSongDto dto : merged) {
            if (dto.getSong() != null && dto.getSong().getId() != null) {
                deduped.putIfAbsent(dto.getSong().getId(), dto);
            }
        }

        // 버전 표기 제거 후 동일 곡명+아티스트 중복 제거 (Live, Acoustic 등)
        Map<String, RecommendedSongDto> versionDeduped = new LinkedHashMap<>();
        for (RecommendedSongDto dto : deduped.values()) {
            String baseKey = normalizeTrackKey(dto.getSong().getTrackName(), dto.getSong().getArtistName());
            versionDeduped.putIfAbsent(baseKey, dto);
        }

        // 1) 버전 트랙 완전 제거 (Live, Acoustic, Remix 등)
        List<RecommendedSongDto> pool = versionDeduped.values().stream()
                .filter(dto -> !isVersionedTrack(dto.getSong().getTrackName()))
                .collect(Collectors.toList());

        // 2) 점수 내림차순 정렬
        pool.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 3) 아티스트별 최대 3곡 제한 (피처링 포함 — "A & B" 에서 A, B 각각 카운트)
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

        if (pool.size() <= limit) {
            Collections.shuffle(pool);
            return pool;
        }

        // 4) 소스별 분리
        List<RecommendedSongDto> vectorPool = new ArrayList<>();
        List<RecommendedSongDto> ytPool    = new ArrayList<>();
        List<RecommendedSongDto> lastFmPool = new ArrayList<>();
        for (RecommendedSongDto dto : pool) {
            if ("ytmusic".equals(dto.getSource()))  ytPool.add(dto);
            else if ("lastfm".equals(dto.getSource())) lastFmPool.add(dto);
            else                                    vectorPool.add(dto);
        }

        // 5) 소스별 최소 슬롯 보장
        int ytSlots     = Math.min(YTMUSIC_MIN_SLOTS, ytPool.size());
        int lastFmSlots = Math.min(YTMUSIC_MIN_SLOTS, lastFmPool.size());
        int vectorSlots = Math.max(0, limit - ytSlots - lastFmSlots);

        // 각 소스 내에서 상위 PINNED_TOP_COUNT 고정 + 나머지 랜덤
        List<RecommendedSongDto> result = new ArrayList<>();
        result.addAll(pickWithVariety(vectorPool,  vectorSlots));
        result.addAll(pickWithVariety(ytPool,      ytSlots));
        result.addAll(pickWithVariety(lastFmPool,  lastFmSlots));

        // 6) 최종 순서 셔플
        Collections.shuffle(result);
        return result;
    }

    // --- private helpers ---

    /** Last.fm 유사곡 → 유사아티스트 순으로 시도 */
    private List<RecommendedSongDto> getLastFmRecommendations(String trackName, String artistName) {
        try {
            var similarTracks = musicApiService.getSimilarTracks(trackName, artistName);
            if (similarTracks != null && similarTracks.getSimilarTracks() != null) {
                List<TrackDTO> tracks = similarTracks.getSimilarTracks().getTrack();
                if (tracks != null && !tracks.isEmpty()) {
                    return tracks.stream()
                            .filter(t -> t.getMatch() >= 0.1)
                            .limit(PER_SOURCE_LIMIT)
                            .map(t -> itunesSongFetcher
                                    .fetchAndSave(t.getName(), t.getArtistDto().getName())
                                    .map(dto -> RecommendedSongDto.builder()
                                            .song(dto)
                                            .reason(String.format(
                                                    "비슷한 분위기의 곡이에요. (유사도 %d%%)",
                                                    (int) (t.getMatch() * 100)))
                                            .source("lastfm")
                                            .score(t.getMatch())
                                            .build())
                                    .orElse(null))
                            .filter(dto -> dto != null)
                            .collect(Collectors.toList());
                }
            }

            // 유사곡 없으면 유사아티스트로 대체
            var similarArtists = musicApiService.getSimilarArtists(artistName);
            if (similarArtists != null && similarArtists.getSimilarArtists() != null) {
                List<ArtistDTO> artists = similarArtists.getSimilarArtists().getArtist();
                if (artists != null) {
                    return artists.stream()
                            .filter(a -> a.getMatch() >= 0.1)
                            .limit(PER_SOURCE_LIMIT)
                            .flatMap(a -> itunesSongFetcher
                                    .fetchAndSave(a.getName(), a.getName())
                                    .map(dto -> List.of(RecommendedSongDto.builder()
                                            .song(dto)
                                            .reason("\"" + artistName + "\"하고 비슷한 아티스트의 곡이에요.")
                                            .source("lastfm")
                                            .score(a.getMatch())
                                            .build()))
                                    .orElse(List.of())
                                    .stream())
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            logger.warn("[RecommendationOrchestrator] Last.fm 실패: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * pool에서 n개를 선택한다.
     * 상위 PINNED_TOP_COUNT는 고정, 나머지는 랜덤 샘플링.
     */
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

    /** 버전 트랙 여부 판별 (Live, Acoustic, Remix 등) */
    private boolean isVersionedTrack(String trackName) {
        if (trackName == null) return false;
        String lower = trackName.toLowerCase();
        return lower.matches(".*\\b(live|acoustic|remix|instrumental|inst|remastered|remaster|radio\\s*edit|version|ver\\.)\\b.*")
                || lower.matches(".*[\\(\\[].*(live|acoustic|remix|instrumental|inst|remastered|반주|노래방|mr).*[\\)\\]].*");
    }

    /**
     * "A feat. B" 등 복합 아티스트명을 개별 키로 분리.
     * 아티스트별 곡 수 제한 시 피처링 포함 여부와 무관하게 동일 아티스트로 집계.
     */
    private List<String> extractArtistKeys(String artistName) {
        if (artistName == null) return List.of("");
        return Arrays.stream(artistName.split("(?i)[&,×]|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bwith\\b"))
                .map(s -> s.trim().replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 곡명에서 버전 표기를 제거해 기본 키 생성.
     */
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

    /** 추천 결과를 AI 챗봇 프롬프트용 텍스트로 변환 */
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
}

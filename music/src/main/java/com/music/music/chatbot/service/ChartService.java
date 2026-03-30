package com.music.music.chatbot.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChartService {

    private static final Logger logger = LoggerFactory.getLogger(ChartService.class);

    // Apple Music RSS Feed — API 키 불필요, 무료
    private static final String KOREA_CHART_URL =
            "https://rss.applemarketingtools.com/api/v2/kr/music/top-songs/20/songs.json";
    private static final String GLOBAL_CHART_URL =
            "https://rss.applemarketingtools.com/api/v2/us/music/top-songs/20/songs.json";

    private final RestClient restClient = RestClient.create();

    // 캐시: 스케줄러가 1시간마다 갱신
    private volatile List<String> chartCache = Collections.emptyList();

    /**
     * 서버 시작 시 최초 1회 차트 로드
     */
    @PostConstruct
    public void init() {
        refreshChart();
    }

    /**
     * 1시간마다 차트 갱신 (매 정각)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshChart() {
        List<String> fetched = fetchChart(KOREA_CHART_URL, "한국");
        if (fetched.isEmpty()) {
            fetched = fetchChart(GLOBAL_CHART_URL, "글로벌"); // 한국 차트 실패 시 미국 차트로 대체
        }
        if (!fetched.isEmpty()) {
            chartCache = fetched;
            logger.info("[ChartService] Apple Music 차트 갱신 완료 - {}곡", fetched.size());
        } else {
            logger.warn("[ChartService] Apple Music 차트 갱신 실패 - 기존 캐시 유지");
        }
    }

    /**
     * 캐시된 한국 인기차트 반환 (비어 있으면 즉시 로드 시도)
     */
    public List<String> getChart() {
        if (chartCache.isEmpty()) {
            refreshChart();
        }
        return chartCache;
    }

    @SuppressWarnings("unchecked")
    private List<String> fetchChart(String url, String region) {
        try {
            Map<String, Object> response = restClient.get().uri(url).retrieve().body(Map.class);
            if (response == null) return Collections.emptyList();

            Object feedObj = response.get("feed");
            if (!(feedObj instanceof Map)) return Collections.emptyList();

            Object resultsObj = ((Map<?, ?>) feedObj).get("results");
            if (!(resultsObj instanceof List)) return Collections.emptyList();

            List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
            List<String> chart = new ArrayList<>();
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);

            int rank = 1;
            for (Map<String, Object> item : results) {
                String name = (String) item.get("name");
                String artist = (String) item.get("artistName");
                String releaseDateStr = (String) item.get("releaseDate");

                // 1년 이내 발매곡만 포함
                if (releaseDateStr != null) {
                    try {
                        LocalDate releaseDate = LocalDate.parse(releaseDateStr);
                        if (releaseDate.isBefore(oneYearAgo)) {
                            logger.debug("[ChartService] 1년 초과 제외: \"{}\" ({})", name, releaseDateStr);
                            continue;
                        }
                    } catch (DateTimeParseException e) {
                        logger.debug("[ChartService] 날짜 파싱 실패, 포함 처리: \"{}\" ({})", name, releaseDateStr);
                    }
                } else {
                    logger.debug("[ChartService] releaseDate 없음, 포함 처리: \"{}\"", name);
                }

                if (name != null && !name.isBlank()) {
                    chart.add(rank++ + ". \"" + name + "\" - " + (artist != null ? artist : "Unknown"));
                }
            }
            logger.info("[ChartService] {} 차트 로드 완료 - {}곡 (필터 후)", region, chart.size());
            chart.forEach(s -> logger.debug("[ChartService] {}", s));
            return chart;
        } catch (Exception e) {
            logger.warn("[ChartService] {} 차트 조회 실패: {}", region, e.getMessage());
            return Collections.emptyList();
        }
    }
}

package com.music.music.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.music.music.ranking.service.RankingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingService rankingService;
    private final RankingCache rankingCache;

    // 서버 시작 시 즉시 1회 실행
    @EventListener(ApplicationReadyEvent.class)
    public void initRanking() {
        log.info("[RankingScheduler] 초기 랭킹 데이터 로딩...");
        refreshAll();
    }

    // 1시간마다 갱신
    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void refreshRanking() {
        log.info("[RankingScheduler] 랭킹 갱신 시작");
        refreshAll();
    }

    private void refreshAll() {
        try {
            rankingCache.setTopLikeUsers(rankingService.getTopLikeUsers());
            rankingCache.setTopPointUsers(rankingService.getTopPointUsers());
            rankingCache.setTopSharedSongs(rankingService.getTopSharedSongs());
            rankingCache.setTopArtists(rankingService.getTopArtists());
            log.info("[RankingScheduler] 랭킹 갱신 완료");
        } catch (Exception e) {
            log.error("[RankingScheduler] 랭킹 갱신 실패", e);
        }
    }
}
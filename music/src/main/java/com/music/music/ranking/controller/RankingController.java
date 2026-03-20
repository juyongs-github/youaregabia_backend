package com.music.music.ranking.controller;

import com.music.music.ranking.RankingCache;
import com.music.music.ranking.dto.ArtistRankingDto;
import com.music.music.ranking.dto.SongRankingDto;
import com.music.music.ranking.dto.UserRankingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingCache rankingCache;

    // 한 번에 전체 반환 (메인 홈 위젯용)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRankings() {
        return ResponseEntity.ok(Map.of(
            "topLikeUsers",   rankingCache.getTopLikeUsers(),
            "topPointUsers",  rankingCache.getTopPointUsers(),
            "topSharedSongs", rankingCache.getTopSharedSongs(),
            "topArtists",     rankingCache.getTopArtists()
        ));
    }

    // 개별 API (필요 시)
    @GetMapping("/like-users")
    public ResponseEntity<List<UserRankingDto>> getLikeUsers() {
        return ResponseEntity.ok(rankingCache.getTopLikeUsers());
    }

    @GetMapping("/point-users")
    public ResponseEntity<List<UserRankingDto>> getPointUsers() {
        return ResponseEntity.ok(rankingCache.getTopPointUsers());
    }

    @GetMapping("/songs")
    public ResponseEntity<List<SongRankingDto>> getSongs() {
        return ResponseEntity.ok(rankingCache.getTopSharedSongs());
    }

    @GetMapping("/artists")
    public ResponseEntity<List<ArtistRankingDto>> getArtists() {
        return ResponseEntity.ok(rankingCache.getTopArtists());
    }
}
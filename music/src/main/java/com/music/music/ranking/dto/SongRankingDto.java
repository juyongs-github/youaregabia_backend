package com.music.music.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SongRankingDto {
    private Long songId;
    private String trackName;
    private String artistName;
    private String imgUrl;
    private long shareCount;    // 공유된 횟수
    private String previewUrl;
    private String genreName;    // 추가
    private Long durationMs;     // 추가
    private String releaseDate;  // 추가
}

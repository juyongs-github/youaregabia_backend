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
}

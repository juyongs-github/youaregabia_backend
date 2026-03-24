package com.music.music.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArtistRankingDto {
    private String artistName;
    private long shareCount;
}

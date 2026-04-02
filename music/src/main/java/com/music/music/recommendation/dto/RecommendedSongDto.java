package com.music.music.recommendation.dto;

import com.music.music.playlist.dto.SongDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendedSongDto {

    private SongDTO song;

    /** 사용자에게 보여줄 추천 이유 */
    private String reason;

    /** 추천 소스: "lastfm" | "vector" */
    private String source;

    /** 유사도 점수 (0.0 ~ 1.0) */
    private double score;
}

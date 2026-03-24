package com.music.music.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRankingDto {
    private Long userId;
    private String name;
    private String grade;       // UserPoint의 Grade
    private long score;         // likeCount 합산 or totalPoint
}

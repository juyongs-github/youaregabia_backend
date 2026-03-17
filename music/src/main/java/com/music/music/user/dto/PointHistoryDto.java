package com.music.music.user.dto;

import java.time.LocalDateTime;

import com.music.music.user.entity.PointHistory;

import lombok.Getter;

@Getter
public class PointHistoryDto {
    private Long id;
    private String pointType;
    private int amount;
    private LocalDateTime createdAt;

    public PointHistoryDto(PointHistory history) {
        this.id = history.getId();
        this.pointType = history.getPointType().name();
        this.amount = history.getAmount();
        this.createdAt = history.getCreatedAt();
    }
}

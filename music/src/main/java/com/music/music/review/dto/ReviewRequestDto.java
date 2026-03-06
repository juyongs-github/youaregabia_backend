package com.music.music.review.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewRequestDto {
    private Long playlistId;
    private String userEmail;
    private Integer rating;
    private String content;
}
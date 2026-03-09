package com.music.music.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class ReviewRequestDto {
    private Long playlistId;
    private String userEmail;
    private Integer rating;
    private String content;
}
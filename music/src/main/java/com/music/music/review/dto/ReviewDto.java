package com.music.music.review.dto;

import com.music.music.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {

    private Long id;
    private Long playlistId;
    private Long userId;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewDto from(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .playlistId(review.getPlaylist().getId())
                .userId(review.getUser().getId())
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
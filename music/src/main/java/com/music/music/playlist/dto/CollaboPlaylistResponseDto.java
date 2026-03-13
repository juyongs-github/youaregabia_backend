package com.music.music.playlist.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboPlaylistResponseDto {

    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String genre;
    private String creatorEmail;
    private String creatorName;
    private int songCount;
    private int participantCount;
    private int likeCount;
    private boolean hasLiked;
    private boolean hasImported;
    private LocalDateTime deadline;
    private boolean deadlinePassed;
    private LocalDateTime createdAt;
}

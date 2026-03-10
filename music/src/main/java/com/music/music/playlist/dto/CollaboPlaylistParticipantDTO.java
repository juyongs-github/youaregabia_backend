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
public class CollaboPlaylistParticipantDTO {

    private Long id;
    private Long songId;
    private String trackName;
    private String artistName;
    private String imgUrl;
    private String suggestedByEmail;
    private LocalDateTime createdAt;
}

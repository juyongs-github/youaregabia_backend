package com.music.music.playlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboSongDto {

    private Long playlistSongId;
    private Long songId;
    private String trackName;
    private String artistName;
    private String imgUrl;
    private String previewUrl;
    private String genreName;
    private String suggestedByName;
    private String suggestedByEmail;
    private int voteCount;
    private boolean hasVoted;
    private String reason;
}

package com.music.music.playlist.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlaylistSongDTO {

    private Long id;

    private Long playlistId;

    private Long songId;

}

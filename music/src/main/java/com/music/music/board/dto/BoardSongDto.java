package com.music.music.board.dto;

import com.music.music.board.entity.BoardSong;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardSongDto {
    private Long songId;
    private String trackName;
    private String artistName;
    private String imgUrl;
    private int orderIndex;
    private String previewUrl;   // 추가
    private String genreName;    // 추가
    private Long durationMs;     // 추가
    private String releaseDate;  // 추가

    public BoardSongDto(BoardSong boardSong) {
        this.songId = boardSong.getSong().getId();
        this.trackName = boardSong.getSong().getTrackName();
        this.artistName = boardSong.getSong().getArtistName();
        this.imgUrl = boardSong.getSong().getImgUrl();
        this.previewUrl = boardSong.getSong().getPreviewUrl(); 
        this.genreName = boardSong.getSong().getGenreName();
        this.durationMs = boardSong.getSong().getDurationMs();
        this.releaseDate = boardSong.getSong().getReleaseDate();
        this.orderIndex = boardSong.getOrderIndex();
    }
}

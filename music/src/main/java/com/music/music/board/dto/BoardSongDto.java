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

    public BoardSongDto(BoardSong boardSong) {
        this.songId = boardSong.getSong().getId();
        this.trackName = boardSong.getSong().getTrackName();
        this.artistName = boardSong.getSong().getArtistName();
        this.imgUrl = boardSong.getSong().getImgUrl();
        this.orderIndex = boardSong.getOrderIndex();
    }
}

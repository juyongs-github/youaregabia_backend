package com.music.music.board.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.music.music.board.common.dto.PageResultDTO;
import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardGenre;
import com.music.music.board.entity.BoardType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDto {
        private Long boardId;
        private String title;
        private String content;
        private String writer;
        private LocalDateTime createdAt;
        private String boardGenre;
        private String boardType;
        private int viewCount;
        private String writerEmail;
        // reply 페이징을 위해서 PageResultDTO로 변경
        private PageResultDTO<ReplyResponseDto> replies;
        private List<BoardSongDto> songs;
        private List<Long> songIds;
        private Long songId;
        private int likeCount;
        private boolean likedByMe;
        private String imgUrl;

public BoardDto(Board board, PageResultDTO<ReplyResponseDto> replies, List<BoardSongDto> songs) {
    this.boardId = board.getBoardId();
    this.title = board.getTitle();
    this.content = board.getContent();
    this.writer = board.getBoardType() == BoardType.FREE
        ? "익명"
        : board.getUser().getName();
    this.writerEmail = board.getUser().getEmail(); // FREE도 항상 내려줌
    this.createdAt = board.getCreatedAt();
    this.boardGenre = board.getBoardGenre().name();
    this.boardType = board.getBoardType().name();
    this.replies = replies;
    this.viewCount = board.getViewCount();
    this.songs = songs;
    this.likeCount = board.getLikeCount();
    this.imgUrl = board.getImgUrl();
}

public BoardDto(Board board) {
    this.boardId = board.getBoardId();
    this.title = board.getTitle();
    this.writer = board.getBoardType() == BoardType.FREE
        ? "익명"
        : board.getUser().getName();
    this.writerEmail = board.getUser().getEmail(); // FREE도 항상 내려줌
    this.createdAt = board.getCreatedAt();
    this.boardType = board.getBoardType().name();
    this.boardGenre = board.getBoardGenre().name();
    this.viewCount = board.getViewCount();
    this.songId = board.getBoardSongs().isEmpty()
        ? null
        : board.getBoardSongs().get(0).getSong().getId();
    this.songs = board.getBoardSongs().stream()
        .map(BoardSongDto::new)
        .toList();
    this.likeCount = board.getLikeCount();
    this.imgUrl = board.getImgUrl();
}
}

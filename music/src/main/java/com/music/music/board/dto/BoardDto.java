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

    public BoardDto(Board board, PageResultDTO<ReplyResponseDto> replies) {
        this.boardId = board.getBoardId();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.writer = board.getBoardType() == BoardType.FREE
        ? "익명"
        : board.getUser().getName();
        this.createdAt = board.getCreatedAt();
        this.boardGenre = board.getBoardGenre().name();
        this.boardType = board.getBoardType().name();
        this.replies = replies;
        this.viewCount = board.getViewCount();
        this.writerEmail = board.getBoardType() == BoardType.FREE
        ? null  // 익명이라 수정/삭제 버튼 안 보이면 안 되니까
        : board.getUser().getEmail();
    }
    
    // 목록용 생성자 (댓글 없음)
    public BoardDto(Board board) {
    this.boardId = board.getBoardId();
    this.title = board.getTitle();
    this.writer = board.getBoardType() == BoardType.FREE
        ? "익명"
        : board.getUser().getName();
    this.createdAt = board.getCreatedAt();
    this.boardType = board.getBoardType().name();
    this.boardGenre = board.getBoardGenre().name();
    this.viewCount = board.getViewCount();
    this.writerEmail = board.getUser().getEmail();
}
    
}

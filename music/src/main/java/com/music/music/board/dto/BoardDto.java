package com.music.music.board.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.music.music.board.entity.Board;

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
    private List<ReplyResponseDto> replies;

    public BoardDto(Board board, List<ReplyResponseDto> replies) {
        this.boardId = board.getBoardId();
        this.title = board.getTitle();
        this.content = board.getContent();
        this.writer = board.getUser().getNickname();
        this.createdAt = board.getCreatedAt();
        this.replies = replies;
    }
    
    public BoardDto(Board board) {
    this.boardId = board.getBoardId();
    this.title = board.getTitle();
    this.writer = board.getUser().getNickname();
    this.createdAt = board.getCreatedAt();
}
    
}

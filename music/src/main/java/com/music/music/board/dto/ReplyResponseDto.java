package com.music.music.board.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.music.music.board.entity.Reply;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class ReplyResponseDto {
    
    private Long replyId;
    private String content;
    private String writer;
    private int likeCount;
    private boolean likedByMe;
    private LocalDateTime createdAt;
    private boolean deleted;
    private String writerEmail;
    private List<ReplyResponseDto> children = new ArrayList<>();

    // ✅ JPQL 전용 생성자
    public ReplyResponseDto(
    Long replyId,
    String content,
    String writer,
    String writerEmail,
    Long likeCount,
    Long likedByMe,
    LocalDateTime createdAt,
    boolean deleted
) {
    this.replyId = replyId;
    this.content = content;
    this.writer = writer;
    this.likeCount = likeCount != null ? likeCount.intValue() : 0;
    this.likedByMe = likedByMe != null && likedByMe > 0;
    this.createdAt = createdAt;
    this.deleted = deleted;
    this.writerEmail = writerEmail;
}


}

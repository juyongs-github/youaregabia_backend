package com.music.music.board.dto;

import java.time.LocalDateTime;

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

    // ✅ JPQL 전용 생성자
    public ReplyResponseDto(
    Long replyId,
    String content,
    String writer,
    Long likeCount,
    Long likedByMe,
    LocalDateTime createdAt
) {
    this.replyId = replyId;
    this.content = content;
    this.writer = writer;
    this.likeCount = likeCount != null ? likeCount.intValue() : 0;
    this.likedByMe = likedByMe != null && likedByMe > 0;
    this.createdAt = createdAt;
}


}

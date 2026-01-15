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
    private LocalDateTime createdAt;

    public ReplyResponseDto(Reply reply) {
        this.replyId = reply.getReplyId();
        this.content = reply.getContent();
        this.writer = reply.getUser().getNickname();
        this.likeCount = reply.getLikeCount();
        this.createdAt = reply.getCreatedAt();
    }


}

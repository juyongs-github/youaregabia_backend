package com.music.music.board.dto;

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
public class ReplyCreateDto {
    private String content;// 댓글 작성 요청 많아질 때를 혹시 몰라 대비해서 분리
    private Long parentReplyId;
}

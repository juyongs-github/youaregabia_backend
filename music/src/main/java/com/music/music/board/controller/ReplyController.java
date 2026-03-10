package com.music.music.board.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.dto.ReplyCreateDto;
import com.music.music.board.service.ReplyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community/share")
public class ReplyController {
  private final ReplyService replyService;

    // 댓글 작성
    @PostMapping("/{boardId}/replies")
    public Long createReply(
            @PathVariable Long boardId,
            @RequestParam String email,
            @RequestBody ReplyCreateDto dto) {
        return replyService.createReply(boardId, email, dto);
    }

    // 댓글 삭제
    @DeleteMapping("/replies/{replyId}")
    public void deleteReply(
            @PathVariable Long replyId,
            @RequestParam String email) {
        replyService.deleteReply(replyId, email);
    }

    @PutMapping("/replies/{replyId}")
    public void updateReply(
        @PathVariable Long replyId,
        @RequestParam String email,
        @RequestBody ReplyCreateDto dto) {
        replyService.updateReply(replyId, email, dto);
    }
}

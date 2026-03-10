package com.music.music.board.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.service.ReplyLikeService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@RequestMapping("/replies")
public class ReplyLikeController {
    private final ReplyLikeService replyLikeService;

    @PostMapping("/{replyId}/like")
    public long toggleLike(@PathVariable Long replyId, @AuthenticationPrincipal String email) {
        return replyLikeService.toggleLike(replyId, email);
    }
    
    
}

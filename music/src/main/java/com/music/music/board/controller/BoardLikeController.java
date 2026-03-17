package com.music.music.board.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.service.BoardLikeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardLikeController {
        private final BoardLikeService boardLikeService;

    @PostMapping("/{boardId}/like")
    public Map<String, Object> toggleLike(
            @PathVariable Long boardId,
            @AuthenticationPrincipal String email) {

        return boardLikeService.toggleLike(boardId, email);
    }
}

package com.music.music.board.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.dto.BoardDto;
import com.music.music.board.service.BoardService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/community/share")
public class BoardController {
    private final BoardService boardService;

    

    @GetMapping
    public List<BoardDto> getBoardList() {
    return boardService.getBoardList();
}
    

    @GetMapping("/{boardId}")
    public BoardDto getBoardDetail(@PathVariable Long boardId,  @RequestParam Long userId) {
        return boardService.getBoardDetail(boardId, userId);
    }

    @PostMapping
    public Long createBoard(@RequestParam Long userId,@RequestBody BoardDto dto) {
        return boardService.createBoard(userId, dto);
}

    @PutMapping("/{boardId}")
    public void updateBoard(
        @PathVariable Long boardId,
        @RequestParam Long userId,
        @RequestBody BoardDto dto) {
        boardService.updateBoard(boardId, userId, dto);
}
    @DeleteMapping("/{boardId}")
    public void deleteBoard(
        @PathVariable Long boardId,
        @RequestParam Long userId) {
        boardService.deleteBoard(boardId, userId);
}
}

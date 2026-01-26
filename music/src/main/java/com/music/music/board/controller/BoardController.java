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
import com.music.music.common.dto.PageRequestDTO;
import com.music.music.common.dto.PageResultDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/community/share")
public class BoardController {
    private final BoardService boardService;

    

    @GetMapping
    public PageResultDTO<BoardDto> getBoardList(PageRequestDTO dto) {
    
        log.info("전체 조회 신청");
        PageResultDTO<BoardDto> result = boardService.getBoardList(dto);
        return result;
}
    

    @GetMapping("/{boardId}")
    public BoardDto getBoardDetail(@PathVariable Long boardId,  @RequestParam Long userId) {
        log.info("상세 조회");
        return boardService.getBoardDetail(boardId, userId);
    }

    @PostMapping
    public Long createBoard(@RequestParam Long userId,@RequestBody BoardDto dto) {
        log.info("게시글 작성");
        return boardService.createBoard(userId, dto);
}

    @PutMapping("/{boardId}")
    public void updateBoard(
        @PathVariable Long boardId,
        @RequestParam Long userId,
        @RequestBody BoardDto dto) {
            log.info("수정 신청");
            boardService.updateBoard(boardId, userId, dto);
}
    @DeleteMapping("/{boardId}")
    public void deleteBoard(
        @PathVariable Long boardId,
        @RequestParam Long userId) {
            log.info("삭제 신청");
            boardService.deleteBoard(boardId, userId);
}
}

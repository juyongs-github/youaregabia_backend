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

import com.music.music.board.common.dto.PageRequestDTO;
import com.music.music.board.common.dto.PageResultDTO;
import com.music.music.board.dto.BoardDto;
import com.music.music.board.service.BoardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/community/share")
public class BoardController {
  private final BoardService boardService;

  @GetMapping("")
  public PageResultDTO<BoardDto> getBoardList(PageRequestDTO dto,
      @RequestParam(name = "keyword", required = false) String keyword) {
    log.info("전체 조회 신청 {}", keyword);
    // 키워드는 필수가 아님
    PageResultDTO<BoardDto> result = boardService.getBoardList(dto, keyword);
    return result;
  }

  @GetMapping("/{boardId}")
  public BoardDto getBoardDetail(@PathVariable("boardId") Long boardId,
      @RequestParam(name = "email", required = false) String email,
      PageRequestDTO dto) {
    log.info("상세 조회 신청 {}, {},{}", boardId, email, dto.getPage());
    return boardService.getBoardDetail(boardId, email, dto);
  }

  @PostMapping("/add")
  public Long createBoard(@RequestParam(name = "email") String email, @RequestBody BoardDto dto) {
    log.info("게시글 생성 {}", dto);
    return boardService.createBoard(email, dto);
  }

  @PutMapping("update/{boardId}")
  public void updateBoard(
      @PathVariable("boardId") Long boardId,
      @RequestParam(name = "email") String email, @RequestBody BoardDto dto) {
    log.info("게시글 수정 {}", dto);
    boardService.updateBoard(boardId, email, dto);
  }

  @DeleteMapping("delete/{boardId}")
  public void deleteBoard(@PathVariable("boardId") Long boardId,
      @RequestParam(name = "email") String email) {
    log.info("게시글 삭제 {}", boardId);
    boardService.deleteBoard(boardId, email);
  }

}

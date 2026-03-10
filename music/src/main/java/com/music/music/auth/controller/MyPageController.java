package com.music.music.auth.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

  private final BoardRepository boardRepository;
  private final ReplyRepository replyRepository;

  // 내가 쓴 게시글 목록
  @GetMapping("/boards")
  public ResponseEntity<List<MyBoardDto>> getMyBoards(Authentication authentication) {
    String email = authentication.getName();
    List<MyBoardDto> result = boardRepository.findByUser_EmailOrderByCreatedAtDesc(email)
        .stream()
        .map(b -> new MyBoardDto(b.getBoardId(), b.getTitle(), b.getBoardType().name(),
            b.getViewCount(), b.getLikeCount(), b.getCreatedAt()))
        .toList();
    return ResponseEntity.ok(result);
  }

  // 내가 쓴 댓글 목록
  @GetMapping("/replies")
  public ResponseEntity<List<MyReplyDto>> getMyReplies(Authentication authentication) {
    String email = authentication.getName();
    List<MyReplyDto> result = replyRepository.findByUser_EmailOrderByCreatedAtDesc(email)
        .stream()
        .map(r -> new MyReplyDto(r.getReplyId(), r.getBoard().getBoardId(),
            r.getBoard().getTitle(), r.getContent(), r.getLikeCount(), r.getCreatedAt()))
        .toList();
    return ResponseEntity.ok(result);
  }

  // DTO 내부 record 정의
  public record MyBoardDto(
      Long boardId,
      String title,
      String boardType,
      int viewCount,
      int likeCount,
      LocalDateTime createdAt) {}

  public record MyReplyDto(
      Long replyId,
      Long boardId,
      String boardTitle,
      String content,
      int likeCount,
      LocalDateTime createdAt) {}
}

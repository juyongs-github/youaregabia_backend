package com.music.music.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.user.entity.Role;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

  private final UserRepository userRepository;
  private final UserLoginLogRepository loginLogRepository;
  private final BoardRepository boardRepository;
  private final ReplyRepository replyRepository;

  // 전체 유저 목록 조회
  @GetMapping("/users")
  public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
    List<UserSummaryDto> users = userRepository.findAll()
        .stream()
        .map(u -> new UserSummaryDto(
            u.getId(), u.getName(), u.getEmail(),
            u.getPhoneNumber(), u.getRole().name(),
            u.getState(), u.getCreatedAt()))
        .toList();
    return ResponseEntity.ok(users);
  }

  // 권한 변경
  @PatchMapping("/users/{id}/role")
  public ResponseEntity<Void> changeRole(
      @PathVariable Long id,
      @RequestBody Map<String, String> body) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
    user.setRole(Role.valueOf(body.get("role")));
    userRepository.save(user);
    return ResponseEntity.ok().build();
  }

  // 접속 로그 조회
  @GetMapping("/logs/login")
  public ResponseEntity<List<UserLoginLog>> getLoginLogs() {
    return ResponseEntity.ok(loginLogRepository.findTop100ByOrderByLoginAtDesc());
  }

  // 활동 로그 조회 (게시글 + 댓글)
  @GetMapping("/logs/activity")
  public ResponseEntity<List<ActivityLogDto>> getActivityLogs() {
    List<ActivityLogDto> combined = new ArrayList<>();

    boardRepository.findAll().stream()
        .filter(b -> !b.isDeleted())
        .forEach(b -> combined.add(new ActivityLogDto(
            "게시글", b.getUser().getName(), b.getUser().getEmail(),
            b.getTitle(), b.getCreatedAt())));

    replyRepository.findAll().stream()
        .filter(r -> !r.isDeleted())
        .forEach(r -> combined.add(new ActivityLogDto(
            "댓글", r.getUser().getName(), r.getUser().getEmail(),
            r.getContent().length() > 30 ? r.getContent().substring(0, 30) + "..." : r.getContent(),
            r.getCreatedAt())));

    combined.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
    return ResponseEntity.ok(combined.subList(0, Math.min(100, combined.size())));
  }

  public record UserSummaryDto(
      Long id,
      String name,
      String email,
      String phoneNumber,
      String role,
      Integer state,
      LocalDateTime createdAt) {}

  public record ActivityLogDto(
      String type,
      String name,
      String email,
      String content,
      LocalDateTime createdAt) {}
}

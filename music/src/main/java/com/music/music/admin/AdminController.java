package com.music.music.admin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.user.entity.Role;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

  private final UserRepository userRepository;

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

  public record UserSummaryDto(
      Long id,
      String name,
      String email,
      String phoneNumber,
      String role,
      Integer state,
      LocalDateTime createdAt) {}
}

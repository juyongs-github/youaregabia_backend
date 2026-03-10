package com.music.music.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.music.music.auth.dto.LoginResponse;
import com.music.music.auth.dto.SocialRegisterRequest;
import com.music.music.auth.jwt.JwtUtil;
import com.music.music.auth.oauth2.OAuth2UserInfo;
import com.music.music.auth.oauth2.PendingSocialStore;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import com.music.music.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/social")
public class SocialAuthController {

  private final PendingSocialStore pendingSocialStore;
  private final UserService userService;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  /**
   * 기존 소셜 유저 로그인 완료 처리
   */
  @GetMapping("/session")
  public ResponseEntity<LoginResponse> session(@RequestParam String token) {
    Long userId = pendingSocialStore.getSession(token);
    if (userId == null) {
      return ResponseEntity.badRequest().build();
    }

    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return ResponseEntity.badRequest().build();
    }

    String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName());

    LoginResponse response = LoginResponse.builder()
        .email(user.getEmail())
        .name(user.getName())
        .createdAt(user.getCreatedAt())
        .imgUrl(user.getImgUrl())
        .token(jwtToken)
        .build();

    return ResponseEntity.ok(response);
  }

  /**
   * CI 중복 시 기존 회원에 소셜 계정 연동 후 로그인 처리
   */
  @PostMapping("/link")
  public ResponseEntity<LoginResponse> link(@RequestBody SocialRegisterRequest request) {
    OAuth2UserInfo pendingInfo = pendingSocialStore.getPending(request.token());
    if (pendingInfo == null) {
      return ResponseEntity.badRequest().build();
    }

    User user = userService.linkSocialUser(request.ci(), pendingInfo);

    String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName());

    LoginResponse response = LoginResponse.builder()
        .email(user.getEmail())
        .name(user.getName())
        .createdAt(user.getCreatedAt())
        .imgUrl(user.getImgUrl())
        .token(jwtToken)
        .build();

    return ResponseEntity.ok(response);
  }

  /**
   * 신규 소셜 유저 회원가입 완료 처리
   */
  @PostMapping("/register")
  public ResponseEntity<LoginResponse> register(@RequestBody SocialRegisterRequest request) {
    OAuth2UserInfo pendingInfo = pendingSocialStore.getPending(request.token());
    if (pendingInfo == null) {
      return ResponseEntity.badRequest().build();
    }

    User user = userService.registerSocialUser(request, pendingInfo);

    String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName());

    LoginResponse response = LoginResponse.builder()
        .email(user.getEmail())
        .name(user.getName())
        .createdAt(user.getCreatedAt())
        .imgUrl(user.getImgUrl())
        .token(jwtToken)
        .build();

    return ResponseEntity.ok(response);
  }
}

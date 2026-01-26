package com.music.music.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.domain.user.repository.UserRepository;
import com.music.music.domain.user.service.UserService;
import com.music.music.dto.auth.LoginRequest;
import com.music.music.dto.auth.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;

  private final UserRepository userRepository;

  // 회원가입
  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    userService.register(request);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 로그인 추가
  @PostMapping("/login")
  public ResponseEntity<Void> login(
      @RequestBody @Valid LoginRequest request) {
    userService.login(request);
    return ResponseEntity.ok().build();
  }

  // 이메일 중복 체크
  @GetMapping("/email-check")
  public boolean checkEmailDuplicate(@RequestParam String email) {
    System.out.println("🔥 email-check called with: " + email);
    return userRepository.existsByEmail(email);
  }

}

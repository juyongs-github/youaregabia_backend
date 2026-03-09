package com.music.music.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.auth.dto.LoginRequest;
import com.music.music.auth.dto.LoginResponse;
import com.music.music.auth.dto.RegisterRequest;
import com.music.music.auth.jwt.JwtUtil;
import com.music.music.auth.service.LocalFileUploader;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import com.music.music.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;
  private final LocalFileUploader fileUploader;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  // 회원가입
  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    userService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 로그인
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
    User user = userService.login(request);

    String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName());

    LoginResponse response = LoginResponse.builder()
        .email(user.getEmail())
        .name(user.getName())
        .createdAt(user.getCreatedAt())
        .imgUrl(user.getImgUrl())
        .token(token)
        .build();

    return ResponseEntity.ok(response);
  }

  // 이메일 중복 체크
  @GetMapping("/email-check")
  public boolean checkEmailDuplicate(@RequestParam("email") String email) {
    System.out.println("email-check called with: " + email);
    return userRepository.existsByEmail(email);
  }

  // 회원탈퇴
  @DeleteMapping("/withdraw")
  public ResponseEntity<Void> withdraw(@RequestBody Map<String, String> body) {
    userService.deleteUser(body.get("email"));
    return ResponseEntity.ok().build();
  }

  // 아이디 찾기
  @PostMapping("/find-email")
  public ResponseEntity<Map<String, String>> findEmail(@RequestBody Map<String, String> body) {
    String maskedEmail = userService.findEmail(body.get("name"), body.get("phoneNumber"));
    return ResponseEntity.ok(Map.of("email", maskedEmail));
  }

  // 비밀번호 재설정
  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> body) {
    userService.resetPassword(body.get("email"), body.get("phoneNumber"), body.get("newPassword"));
    return ResponseEntity.ok().build();
  }

  // 이미지 업로드
  @PatchMapping("/update-image")
  public ResponseEntity<Map<String, String>> updateImage(
      @RequestParam("email") String email,
      @RequestPart("image") MultipartFile image) {
    String imageUrl = fileUploader.upload(image, "profile");
    userService.updateProfileImage(email, imageUrl);
    return ResponseEntity.ok(Map.of("imgUrl", imageUrl));
  }
}

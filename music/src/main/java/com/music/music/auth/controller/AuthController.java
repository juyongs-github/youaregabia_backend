package com.music.music.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
<<<<<<< HEAD
=======
import org.springframework.web.multipart.MultipartFile;
>>>>>>> origin/feature/jylee_2

import com.music.music.auth.dto.LoginRequest;
import com.music.music.auth.dto.LoginResponse;
import com.music.music.auth.dto.RegisterRequest;
<<<<<<< HEAD
import com.music.music.user.entitiy.User;
=======
import com.music.music.auth.jwt.JwtUtil;
import com.music.music.auth.service.LocalFileUploader;
import com.music.music.user.entity.User;
>>>>>>> origin/feature/jylee_2
import com.music.music.user.repository.UserRepository;
import com.music.music.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

<<<<<<< HEAD
=======
import java.util.Map;

>>>>>>> origin/feature/jylee_2
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
<<<<<<< HEAD
import org.springframework.web.bind.annotation.RequestParam;
=======
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
>>>>>>> origin/feature/jylee_2

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;
<<<<<<< HEAD

  private final UserRepository userRepository;
=======
  private final LocalFileUploader fileUploader;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
>>>>>>> origin/feature/jylee_2

  // 회원가입
  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    userService.register(request);
<<<<<<< HEAD

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 로그인 추가
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
    // 1. UserService에서 로그인 처리를 하고 유저 객체를 받아옵니다.
    // (UserService.login의 리턴 타입을 User로 바꿔야 합니다)
    User user = userService.login(request);

    // 2. 응답용 DTO(LoginResponse)에 담습니다.
    LoginResponse response = LoginResponse.builder()
        .email(user.getEmail())
        .name(user.getName()) // User 엔티티의 name 필드
        .createdAt(user.getCreatedAt()) // User 엔티티의 createdAt 필드
        .build();

    // 3. 데이터를 담아서 200 OK 응답을 보냅니다.
=======
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

>>>>>>> origin/feature/jylee_2
    return ResponseEntity.ok(response);
  }

  // 이메일 중복 체크
  @GetMapping("/email-check")
  public boolean checkEmailDuplicate(@RequestParam("email") String email) {
<<<<<<< HEAD
    System.out.println("🔥 email-check called with: " + email);
    return userRepository.existsByEmail(email);
  }

=======
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
>>>>>>> origin/feature/jylee_2
}

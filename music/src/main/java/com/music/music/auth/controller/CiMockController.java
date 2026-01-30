package com.music.music.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.music.music.auth.dto.CiGenerator;
import com.music.music.auth.dto.CiVerifyRequest;
import com.music.music.auth.dto.CiVerifyResponse;
import com.music.music.auth.service.SmsVerificationService;
import com.music.music.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ci/mock")
@Validated
public class CiMockController {

  private final SmsVerificationService smsVerificationService;
  private final UserRepository userRepository;

  @PostMapping("/verify")
  public ResponseEntity<CiVerifyResponse> verify(@Valid @RequestBody CiVerifyRequest request) {

    // 1) SMS 인증 완료 여부 강제
    if (!smsVerificationService.isPhoneVerified(request.phoneNumber())) {
      return ResponseEntity.badRequest()
          .body(new CiVerifyResponse(false, null, false, "SMS 인증이 완료되지 않았습니다."));
    }

    // 2) CI 생성
    String ci = CiGenerator.generate(request.name(), request.birthDate(), request.phoneNumber());

    // 3) DB에 CI가 이미 존재하는지 확인 (로컬/소셜 구분 없이 단일 기준)
    boolean exists = userRepository.existsByCi(ci);

    if (exists) {
      // ✅ 기존회원이면 가입 진행 막는 용도
      return ResponseEntity.ok(new CiVerifyResponse(true, ci, true, "기존 회원입니다. 로그인 후 이용해주세요."));
    }

    // 4) 신규면 가입폼으로 진행 가능
    // 선택: CI 발급 후 verified 해제(1회성)
    // smsVerificationService.clearVerified(request.phone());

    return ResponseEntity.ok(new CiVerifyResponse(true, ci, false, null));
  }
}

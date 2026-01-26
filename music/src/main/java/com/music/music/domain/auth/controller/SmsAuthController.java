package com.music.music.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.domain.auth.dto.SmsSendRequest;
import com.music.music.domain.auth.dto.SmsSendResponse;
import com.music.music.domain.auth.dto.SmsVerifyRequest;
import com.music.music.domain.auth.service.SmsVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/sms")
@Validated
public class SmsAuthController {

  private final SmsVerificationService smsVerificationService;

  @PostMapping("/send")
  public ResponseEntity<SmsSendResponse> send(@Valid @RequestBody SmsSendRequest request) {
    var result = smsVerificationService.generateAndStoreCode(request.getPhoneNumber());

    // Mock 단계에서 mockCode를 내려준다
    return ResponseEntity.ok(new SmsSendResponse(result.success(), result.message(), result.mockCode()));
  }

  @PostMapping("/verify")
  public ResponseEntity<?> verify(@Valid @RequestBody SmsVerifyRequest request) {
    var result = smsVerificationService.verifyCode(request.getPhoneNumber(), request.getCode());

    return ResponseEntity.ok(result);
    // { "success": true/false, "message": "..." }
  }

  @GetMapping("/ping")
  public String ping() {
    return "sms pong";
  }

}

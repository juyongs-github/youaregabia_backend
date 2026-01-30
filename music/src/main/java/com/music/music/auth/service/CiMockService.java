package com.music.music.auth.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

import com.music.music.auth.dto.CiGenerator;
import com.music.music.auth.dto.CiVerifyRequest;

@Service
public class CiMockService {
  public String verifyAndIssueCi(CiVerifyRequest req) {
    // 1) phone digits
    String phoneDigits = req.phoneNumber().replaceAll("\\D", "");

    // 최소 검증 (원하면 더 빡세게 가능)
    if (!phoneDigits.matches("^01[016789]\\d{7,8}$")) {
      throw new IllegalArgumentException("휴대폰 번호 형식이 올바르지 않습니다.");
    }

    // 2) birthDate parse + 범위 검증
    LocalDate birth = parseBirthDate(req.birthDate());
    LocalDate min = LocalDate.of(1920, 1, 1);
    LocalDate max = LocalDate.of(2030, 12, 31);
    if (birth.isBefore(min) || birth.isAfter(max)) {
      throw new IllegalArgumentException("생년월일은 1920-01-01 ~ 2030-12-31 사이여야 합니다.");
    }

    // 3) CI 생성
    return CiGenerator.generate(req.name(), req.birthDate(), phoneDigits);
  }

  private LocalDate parseBirthDate(String value) {
    try {
      return LocalDate.parse(value); // "YYYY-MM-DD"
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("유효하지 않은 생년월일입니다.");
    }
  }
}

package com.music.music.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CiVerifyRequest(
    @NotBlank(message = "이름은 필수입니다.") String name,

    // YYYY-MM-DD 또는 YYYYMMDD 중 택1로 맞춰야 함
    // 프론트 CI 페이지에서 YYYYMMDD로 보내려면 아래 정규식 변경 필요
    // 여기서는 YYYY-MM-DD로 통일 추천
    @NotBlank(message = "생년월일은 필수입니다.") @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일 형식은 yyyy-MM-dd 여야 합니다.") String birthDate,

    @NotBlank(message = "휴대폰 번호는 필수입니다.") @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.") String phoneNumber) {
}

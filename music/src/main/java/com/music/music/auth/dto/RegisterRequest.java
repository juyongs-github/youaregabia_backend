package com.music.music.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RegisterRequest {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, max = 30, message = "비밀번호는 8~30자여야 합니다.")
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  @Size(min = 2, max = 10, message = "이름은 2~20자여야 합니다.")
  @Pattern(regexp = "^[가-힣]{2,10}$", message = "이름은 한글 2~10자로 입력해주세요.")
  private String name;

  // ✅ 추가 (형식 검증 전용)
  @NotBlank(message = "생년월일은 필수입니다.")
  @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "생년월일 형식은 yyyy-MM-dd 여야 합니다.")
  private String birthDate;

  @NotBlank(message = "휴대폰 번호는 필수입니다.")
  // 010-1234-5678 or 01012345678 둘 다 허용
  @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
  private String phoneNumber;

  @NotBlank(message = "주소는 필수입니다.")
  private String address;

  // ✅ [ADD] CI (본인인증 결과값)
  @NotBlank(message = "본인인증이 완료되지 않았습니다.")
  private String ci;

}

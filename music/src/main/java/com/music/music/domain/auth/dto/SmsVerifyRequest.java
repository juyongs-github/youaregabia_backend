package com.music.music.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SmsVerifyRequest {

  @NotBlank(message = "휴대폰 번호는 필수입니다.")
  @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
  private String phoneNumber;

  @NotBlank(message = "인증번호는 필수입니다.")
  @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
  private String code;

  public SmsVerifyRequest() {
  }

  public SmsVerifyRequest(String phoneNumber, String code) {
    this.phoneNumber = phoneNumber;
    this.code = code;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}

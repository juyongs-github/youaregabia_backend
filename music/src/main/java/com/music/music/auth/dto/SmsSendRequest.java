package com.music.music.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SmsSendRequest {

  @NotBlank(message = "휴대폰 번호는 필수입니다.")
  // 010-1234-5678 또는 01012345678 둘 다 허용하고 싶으면 아래 정규식 유지
  @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
  private String phoneNumber;

  public SmsSendRequest() {
  }

  public SmsSendRequest(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }
}

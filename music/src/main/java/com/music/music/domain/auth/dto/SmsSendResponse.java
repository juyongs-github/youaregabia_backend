package com.music.music.domain.auth.dto;

public class SmsSendResponse {

  private boolean success;
  private String message;

  // ✅ Mock 단계에서만 내려줄 것
  private String mockCode;

  public SmsSendResponse() {
  }

  public SmsSendResponse(boolean success, String message, String mockCode) {
    this.success = success;
    this.message = message;
    this.mockCode = mockCode;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public String getMockCode() {
    return mockCode;
  }
}

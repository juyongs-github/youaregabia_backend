package com.music.music.auth.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
  private String email;
  private String name;
  private LocalDateTime createdAt;
  private String imgUrl;
  private String token;
}

package com.music.music.auth.dto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class CiGenerator {

  private CiGenerator() {
  }

  // 서버에서만 아는 "pepper" (환경변수로 빼는 게 정석)
  private static final String PEPPER = "CI_MOCK_PEPPER_CHANGE_ME";

  public static String generate(String name, String birthDate, String phoneDigits) {
    String normalized = normalize(name, birthDate, phoneDigits);

    // SHA-256 해시 -> Base64Url 인코딩 (길이 짧고 URL 안전)
    byte[] hash = sha256((normalized + "|" + PEPPER).getBytes(StandardCharsets.UTF_8));
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

    // 버전 붙여두면 나중에 규칙 바꿔도 관리 쉬움
    return "CI_MOCK_v1_" + token;
  }

  private static String normalize(String name, String birthDate, String phoneDigits) {
    String n = name == null ? "" : name.trim();
    String b = birthDate == null ? "" : birthDate.trim();
    String p = phoneDigits == null ? "" : phoneDigits.replaceAll("\\D", "");
    return (n + "|" + b + "|" + p).toLowerCase();
  }

  private static byte[] sha256(byte[] input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return md.digest(input);
    } catch (Exception e) {
      throw new IllegalStateException("CI 해시 생성 실패", e);
    }
  }

}

package com.music.music.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class SmsVerificationService {

  private static final long EXPIRE_SECONDS = 180; // 3분
  private static final SecureRandom random = new SecureRandom();

  /**
   * key: phoneNumber(normalized)
   * value: VerificationData(code, expireAt)
   */
  private final Map<String, VerificationData> store = new ConcurrentHashMap<>();

  /**
   * ✅ SMS 인증 완료된 번호 저장소
   * - CI 발급은 이 verified를 통과한 번호만 가능
   */
  private final Set<String> verified = ConcurrentHashMap.newKeySet();

  public SmsSendResult generateAndStoreCode(String rawPhoneNumber) {
    String phone = normalize(rawPhoneNumber);

    String code = generate6Digits();
    Instant expireAt = Instant.now().plusSeconds(EXPIRE_SECONDS);

    store.put(phone, new VerificationData(code, expireAt));

    // Mock이므로 서버 로그에 찍는다 (실서비스 절대 금지)
    System.out.println("[MOCK-SMS] phone=" + phone + ", code=" + code + ", expireAt=" + expireAt);

    return new SmsSendResult(true, "인증번호가 발송되었습니다.", code);
  }

  public VerifyResult verifyCode(String rawPhoneNumber, String inputCode) {
    String phone = normalize(rawPhoneNumber);

    VerificationData data = store.get(phone);
    if (data == null) {
      return new VerifyResult(false, "인증번호를 먼저 요청해주세요.");
    }

    if (Instant.now().isAfter(data.expireAt())) {
      store.remove(phone);
      return new VerifyResult(false, "인증번호가 만료되었습니다. 다시 요청해주세요.");
    }

    if (!data.code().equals(inputCode)) {
      return new VerifyResult(false, "인증번호가 일치하지 않습니다.");
    }

    // ✅ 성공: 1회성 코드 삭제
    store.remove(phone);

    // ✅ 성공: verified 처리 (CI 발급 가능)
    verified.add(phone);

    return new VerifyResult(true, "휴대폰 인증이 완료되었습니다.");
  }

  // ✅ CI 컨트롤러에서 사용
  public boolean isPhoneVerified(String rawPhoneNumber) {
    String phone = normalize(rawPhoneNumber);
    return verified.contains(phone);
  }

  // 선택: CI 발급 후 1회성으로 막고 싶으면 사용
  public void clearVerified(String rawPhoneNumber) {
    String phone = normalize(rawPhoneNumber);
    verified.remove(phone);
  }

  /** 010-1234-5678 -> 01012345678 */
  public String normalize(String rawPhoneNumber) {
    if (rawPhoneNumber == null)
      return "";
    return rawPhoneNumber.replaceAll("-", "").trim();
  }

  private String generate6Digits() {
    int n = random.nextInt(1_000_000); // 0 ~ 999999
    return String.format("%06d", n);
  }

  public record VerificationData(String code, Instant expireAt) {
  }

  public record SmsSendResult(boolean success, String message, String mockCode) {
  }

  public record VerifyResult(boolean success, String message) {
  }
}

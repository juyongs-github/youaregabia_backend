package com.music.music.user.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.auth.dto.LoginRequest;
import com.music.music.auth.dto.RegisterRequest;
import com.music.music.auth.dto.SocialRegisterRequest;
import com.music.music.auth.oauth2.OAuth2UserInfo;
import com.music.music.user.entity.User;
import com.music.music.user.entity.UserSocialAccount;
import com.music.music.user.repository.UserRepository;
import com.music.music.user.repository.UserSocialAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final UserSocialAccountRepository socialAccountRepository;
  private final PasswordEncoder passwordEncoder;

  private static final LocalDate MIN_BIRTH_DATE = LocalDate.of(1920, 1, 1);

  private static final LocalDate MAX_BIRTH_DATE = LocalDate.of(2030, 12, 31);

  private LocalDate validateAndParseBirthDate(String birthDate) {
    LocalDate parsed;

    try {
      parsed = LocalDate.parse(birthDate);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("생년월일 형식이 올바르지 않습니다.");
    }

    if (parsed.isBefore(MIN_BIRTH_DATE) || parsed.isAfter(MAX_BIRTH_DATE)) {
      throw new IllegalArgumentException(
          "생년월일은 1920-01-01 ~ 2030-12-31 사이여야 합니다.");
    }

    return parsed;
  }

  public User register(RegisterRequest request) {

    // 1️⃣ CI 필수 체크
    if (request.getCi() == null || request.getCi().isBlank()) {
      throw new IllegalArgumentException("본인인증(CI)이 필요합니다.");
    }

    // 2️⃣ CI 중복 체크
    if (userRepository.existsByCi(request.getCi())) {
      throw new IllegalArgumentException("이미 가입된 사용자입니다.");
    }

    // [ADD] 이메일 normalize(대소문자 혼합 중복 방지)
    String normalizedEmail = normalizeEmail(request.getEmail());

    if (userRepository.existsByEmail(normalizedEmail)) {
      // [NOTE] 추후 ControllerAdvice로 에러 포맷 통일 추천
      throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    String normalizedPhone = normalizePhone(request.getPhoneNumber());

    // ✅ 3. 생년월일 검증 + 파싱 (핵심)
    LocalDate birthDate = validateAndParseBirthDate(request.getBirthDate());
    System.out.println("🔥 validateAndParseBirthDate called");

    User user = User.builder()
        .email(normalizedEmail) // [FIX] normalize된 이메일 저장
        .password(encodedPassword)
        .name(request.getName())
        .birthDate(birthDate) // 반드시 들어가야 함
        .phoneNumber(normalizedPhone)
        .address(request.getAddress())
        .ci(request.getCi()) // CI값 추가
        .build();

    return userRepository.save(user);

  }

  // [FIX] 로그인은 읽기 전용으로 의미/성능 정리
  @Transactional(readOnly = true)
  public User login(LoginRequest request) {
    String normalizedEmail = normalizeEmail(request.getEmail());

    User user = userRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
    }

    // JWT는 다음 단계에서 여기서 토큰 생성/반환(또는 Controller에서 처리)
    return user;
  }

  private String normalizeEmail(String email) {
    if (email == null)
      return null;
    return email.trim().toLowerCase();
  }

  // 휴대폰 번호 정규화
  private String normalizePhone(String phone) {
    if (phone == null)
      return null;

    String digits = phone.replaceAll("\\D", "");
    if (digits.length() == 11) {
      return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
    }
    if (digits.length() == 10) {
      return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
    }
    return phone;
  }

  public User registerSocialUser(SocialRegisterRequest request, OAuth2UserInfo oAuth2UserInfo) {
    if (userRepository.existsByCi(request.ci())) {
      throw new IllegalArgumentException("이미 가입된 사용자입니다.");
    }

    LocalDate birthDate = validateAndParseBirthDate(request.birthDate());
    String normalizedPhone = normalizePhone(request.phoneNumber());
    String normalizedEmail = normalizeEmail(oAuth2UserInfo.email());

    User user = User.builder()
        .email(normalizedEmail)
        .name(request.name())
        .birthDate(birthDate)
        .phoneNumber(normalizedPhone)
        .ci(request.ci())
        .build();

    userRepository.save(user);

    UserSocialAccount socialAccount = UserSocialAccount.builder()
        .user(user)
        .provider(oAuth2UserInfo.provider())
        .providerId(oAuth2UserInfo.providerId())
        .build();

    socialAccountRepository.save(socialAccount);

    return user;
  }

  public User linkSocialUser(String ci, OAuth2UserInfo oAuth2UserInfo) {
    User user = userRepository.findByCi(ci)
        .orElseThrow(() -> new IllegalArgumentException("해당 CI의 사용자를 찾을 수 없습니다."));

    boolean alreadyLinked = socialAccountRepository
        .findByProviderAndProviderId(oAuth2UserInfo.provider(), oAuth2UserInfo.providerId())
        .isPresent();

    if (!alreadyLinked) {
      UserSocialAccount socialAccount = UserSocialAccount.builder()
          .user(user)
          .provider(oAuth2UserInfo.provider())
          .providerId(oAuth2UserInfo.providerId())
          .build();
      socialAccountRepository.save(socialAccount);
    }

    return user;
  }

  @Transactional
  public void deleteUser(String email) {
    String normalizedEmail = normalizeEmail(email);
    User user = userRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    userRepository.delete(user);
  }

  @Transactional(readOnly = true)
  public String findEmail(String name, String phoneNumber) {
    String normalizedPhone = normalizePhone(phoneNumber);
    User user = userRepository.findByNameAndPhoneNumber(name, normalizedPhone)
        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
    return maskEmail(user.getEmail());
  }

  @Transactional
  public void resetPassword(String email, String phoneNumber, String newPassword) {
    String normalizedEmail = normalizeEmail(email);
    String normalizedPhone = normalizePhone(phoneNumber);
    User user = userRepository.findByEmailAndPhoneNumber(normalizedEmail, normalizedPhone)
        .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
    user.setPassword(passwordEncoder.encode(newPassword));
  }

  private String maskEmail(String email) {
    int atIdx = email.indexOf('@');
    if (atIdx <= 2) return email;
    String local = email.substring(0, atIdx);
    String domain = email.substring(atIdx);
    String visible = local.substring(0, 2);
    String masked = "*".repeat(local.length() - 2);
    return visible + masked + domain;
  }

  @Transactional
  public void updateProfileImage(String email, String imgUrl) {
    // 1. 기존에 구현하신 normalizeEmail을 사용하여 유저를 찾습니다.
    User user = userRepository.findByEmail(normalizeEmail(email))
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    // 2. 찾아온 유저 엔티티의 imgUrl을 업데이트합니다.
    user.setImgUrl(imgUrl);

    // @Transactional 어노테이션 덕분에 메서드가 끝날 때 DB에 자동 저장됩니다.
  }
}

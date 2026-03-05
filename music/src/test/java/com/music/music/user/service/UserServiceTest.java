package com.music.music.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.auth.dto.LoginRequest;
import com.music.music.auth.dto.RegisterRequest;
import com.music.music.user.entitiy.User;
import com.music.music.user.repository.UserRepository;
import com.music.music.user.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

@SpringBootTest
@Transactional
@Rollback(false)
public class UserServiceTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("회원가입 성공: 이메일이 중복이 아니면 저장된다")
  void registerSuccess() {
    // given
    RegisterRequest request = new RegisterRequest(
        "test@test.com",
        "password123",
        "테스트유저",
        "1996-03-03", // ✅ 추가
        "010-1234-5678",
        "서울",
        "MOCK-CI-EXIST");

    // when
    User saved = userService.register(request);

    // then
    assertThat(saved.getId()).isNotNull();
    assertThat(userRepository.existsByEmail("test@test.com")).isTrue();
  }

  @Test
  @DisplayName("이메일 중복 회원가입 실패")
  void registerDuplicateFail() {
    userRepository.save(User.builder()
        .email("test@test.com")
        .password("password")
        .name("기존유저")
        .phoneNumber("010-1111-2222")
        .address("서울")
        .ci("ajdktsj")
        .build());

    RegisterRequest request = new RegisterRequest(
        "test@test.com",
        "password123",
        "테스트유저",
        "1996-03-03", // ✅ 추가
        "010-1234-5678",
        "서울",
        "MOCK-CI-EXIST");

    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("이미 존재하는 이메일입니다.");
  }

  @Test
  @DisplayName("로그인 성공")
  void loginSuccess() {
    userRepository.save(User.builder()
        .email("test@test.com")
        .password(new BCryptPasswordEncoder().encode("password123"))
        .name("테스트유저")
        .phoneNumber("010-1234-5678")
        .address("서울")
        .ci("cafd")
        .build());

    LoginRequest request = new LoginRequest("test@test.com", "password123");

    User user = userService.login(request);

    assertThat(user.getEmail()).isEqualTo("test@test.com");
  }

  @Test
  @DisplayName("없는 유저 로그인 실패")
  void loginFail() {
    LoginRequest request = new LoginRequest("no@test.com", "password");

    assertThatThrownBy(() -> userService.login(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("존재하지 않는 사용자입니다.");
  }
}

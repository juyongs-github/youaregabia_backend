package com.music.music.user.repository;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

<<<<<<< HEAD
import com.music.music.user.entitiy.User;
=======
import com.music.music.user.entity.User;
>>>>>>> origin/feature/jylee_2
import com.music.music.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Rollback(false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositorytest {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("회원가입 성공시 사용자 정보가 DB에 정상 저장된다")
  void saveUserTest() {
    // given
    User user = User.builder()
        .email("test@test.com")
        .password("password123")
        .name("테스트유저")
        .phoneNumber("010-1234-5678")
        .address("테스트나라에살아요")
        .createdAt(LocalDateTime.now())
        .ci("dfdfasfe")
        .build();

    // when
    User savedUser = userRepository.save(user);

    // then
    assertThat(savedUser.getEmail()).isEqualTo("test@test.com");
    assertThat(savedUser.getName()).isEqualTo("테스트유저");
  }

}

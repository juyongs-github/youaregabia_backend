package com.music.music.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.music.music.domain.user.entitiy.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * 로그인 / 인증에서 가장 많이 씀.
   * email로 사용자 1명 조회 (없으면 Optional.empty()).
   */
  Optional<User> findByEmail(String email);

  /**
   * 회원가입 시 중복 체크용.
   * 이미 존재하면 true
   */
  boolean existsByEmail(String email);

  /**
   * 탈퇴/정리 같은 케이스에서 사용 가능 (선택).
   * 삭제된 row 수를 반환.
   */
  long deleteByEmail(String email);
}

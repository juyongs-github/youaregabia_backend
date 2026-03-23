package com.music.music.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.user.entity.UserPoint;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUser_Id(Long userId);
    Optional<UserPoint> findByUser_Email(String email);

    // 회원탈퇴 시 삭제
    void deleteByUser_Id(Long userId);
}

package com.music.music.admin;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {
    List<UserLoginLog> findTop100ByOrderByLoginAtDesc();
}

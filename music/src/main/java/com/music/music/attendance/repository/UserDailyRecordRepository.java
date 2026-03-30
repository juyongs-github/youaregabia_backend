package com.music.music.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.attendance.entity.UserDailyRecord;
import com.music.music.user.entity.User;

public interface UserDailyRecordRepository extends JpaRepository<UserDailyRecord, Long> {

    Optional<UserDailyRecord> findByUserAndRecordDate(User user, LocalDate date);

    // 이번 달 출석 기록 (달력 UI용)
    List<UserDailyRecord> findByUserAndRecordDateBetween(User user, LocalDate from, LocalDate to);
}
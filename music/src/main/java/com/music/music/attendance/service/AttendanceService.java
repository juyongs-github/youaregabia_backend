package com.music.music.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.attendance.dto.AttendanceResponse;
import com.music.music.attendance.dto.AttendanceStatusResponse;
import com.music.music.attendance.entity.UserDailyRecord;
import com.music.music.attendance.repository.UserDailyRecordRepository;
import com.music.music.user.entity.PointType;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import com.music.music.user.service.UserPointService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final UserDailyRecordRepository userDailyRecordRepository;
    private final UserPointService userPointService;
    private final UserRepository userRepository;

    private static final int POINT_ATTENDANCE = 50;
    private static final int POINT_STREAK_7 = 300;

    /** 출석 체크 */
    public AttendanceResponse checkAttendance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        LocalDate today = LocalDate.now();

        // 오늘 이미 출석했으면 중복 방지
        if (userDailyRecordRepository.findByUserAndRecordDate(user, today).isPresent()) {
            return AttendanceResponse.alreadyChecked();
        }

        // 연속 출석 계산
        int streak = calcStreak(user, today);

        // 출석 기록 저장
        userDailyRecordRepository.save(UserDailyRecord.of(user, today, streak));

        // 출석 포인트 지급 (UserPointService 기존 패턴 그대로)
        userPointService.addPoint(email, PointType.ATTENDANCE);

        // 7일 배수마다 보너스
        int bonusPoint = 0;
        if (streak % 7 == 0) {
            userPointService.addPoint(email, PointType.ATTENDANCE_7);
            bonusPoint = POINT_STREAK_7;
        }

        return AttendanceResponse.success(streak, POINT_ATTENDANCE, bonusPoint);
    }

    /** 오늘 출석 여부 + streak 조회 */
    @Transactional(readOnly = true)
    public AttendanceStatusResponse getStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        LocalDate today = LocalDate.now();
        Optional<UserDailyRecord> todayRecord =
                userDailyRecordRepository.findByUserAndRecordDate(user, today);

        boolean checked = todayRecord.isPresent();
        int streak = todayRecord.map(UserDailyRecord::getStreakCount).orElse(0);

        return new AttendanceStatusResponse(checked, streak);
    }

    /** 이번 달 출석 기록 조회 (달력용) */
    @Transactional(readOnly = true)
    public List<LocalDate> getMonthlyAttendance(String email, int year, int month) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        return userDailyRecordRepository
                .findByUserAndRecordDateBetween(user, from, to)
                .stream()
                .map(UserDailyRecord::getRecordDate)
                .toList();
    }

    /** 연속 출석일 계산 */
    private int calcStreak(User user, LocalDate today) {
        Optional<UserDailyRecord> yesterday =
                userDailyRecordRepository.findByUserAndRecordDate(user, today.minusDays(1));
        return yesterday.map(r -> r.getStreakCount() + 1).orElse(1);
    }
}
package com.music.music.attendance.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.attendance.dto.AttendanceResponse;
import com.music.music.attendance.dto.AttendanceStatusResponse;
import com.music.music.attendance.service.AttendanceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 출석 체크
    @PostMapping("")
    public ResponseEntity<AttendanceResponse> checkAttendance(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(attendanceService.checkAttendance(email));
    }

    // 오늘 출석 여부 + streak 조회
    @GetMapping("/status")
    public ResponseEntity<AttendanceStatusResponse> getStatus(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(attendanceService.getStatus(email));
    }

    // 이번 달 출석 달력
    @GetMapping("/calendar")
    public ResponseEntity<List<LocalDate>> getCalendar(
            @AuthenticationPrincipal String email,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(attendanceService.getMonthlyAttendance(email, year, month));
    }
}

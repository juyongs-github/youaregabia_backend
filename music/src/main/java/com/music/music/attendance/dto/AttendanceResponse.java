package com.music.music.attendance.dto;

public record AttendanceResponse(
        boolean isAlreadyChecked,
        int streak,
        int point,
        int bonusPoint
) {
    public static AttendanceResponse alreadyChecked() {
        return new AttendanceResponse(true, 0, 0, 0);
    }
    public static AttendanceResponse success(int streak, int point, int bonus) {
        return new AttendanceResponse(false, streak, point, bonus);
    }
}
package com.music.music.attendance.dto;

public record AttendanceStatusResponse(
        boolean checked,
        int streak
) {}
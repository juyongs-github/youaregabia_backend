package com.music.music.attendance.entity;

import java.time.LocalDate;

import com.music.music.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class UserDailyRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDate recordDate;

    private int streakCount; // 연속 출석일

    public static UserDailyRecord of(User user, LocalDate date, int streak) {
        UserDailyRecord r = new UserDailyRecord();
        r.user = user;
        r.recordDate = date;
        r.streakCount = streak;
        return r;
    }
}
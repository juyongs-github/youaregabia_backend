package com.music.music.user.entity;

import lombok.Getter;

@Getter
public enum PointType {
    BOARD_WRITE(10, "게시글 작성"),
    REPLY_WRITE(5, "댓글 작성"),
    CHILD_REPLY_WRITE(3, "대댓글 작성"),
    MUSIC_QUIZ(1, "음악 퀴즈"),
    ALBUM_QUIZ(1, "앨범 퀴즈"),
    CARD_QUIZ(1, "카드 퀴즈"),
    CRITIC_WRITE(20, "평론 작성"),
    LIKE_GIVEN(1,    "좋아요 누르기"),
    POINT_DEDUCT(-1, "포인트 차감"),
    ADMIN_GRANT(0,   "관리자 포인트 지급"),
    ADMIN_DEDUCT(0,  "관리자 포인트 차감"),
    ATTENDANCE(50,"매일 출석"),      // 매일 출석 (50포인트)
    ATTENDANCE_7(300,"7일 연속 출석");    // 연속 7일 보너스 (300포인트)

    private final int defaultAmount;
    private final String description;

    PointType(int defaultAmount, String description) {
        this.defaultAmount = defaultAmount;
        this.description = description;
    }
}

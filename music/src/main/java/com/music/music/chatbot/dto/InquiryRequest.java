package com.music.music.chatbot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryRequest {
    private String type;    // "계정 문제", "음악 재생 오류", "기타 문의"
    private String content;
    private String email;   // 비로그인 사용자가 직접 입력한 이메일 (nullable)
}

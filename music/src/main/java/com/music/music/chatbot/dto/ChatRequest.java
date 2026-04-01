package com.music.music.chatbot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequest {
    private String message;
    private String sessionId; // 기존 세션 이어가기 (없으면 새 세션 생성)
}

package com.music.music.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String type;      // "rule" or "ai"
    private String sessionId; // 프론트가 다음 요청에 사용
}

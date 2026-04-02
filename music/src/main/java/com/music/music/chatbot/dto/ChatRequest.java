package com.music.music.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequest {
    @NotBlank(message = "질문을 입력해주세요.")
    @Size(max = 300, message = "질문은 300자 이내로 입력해주세요.")
    private String message;

    private String sessionId; // 기존 세션 이어가기 (없으면 새 세션 생성)
}

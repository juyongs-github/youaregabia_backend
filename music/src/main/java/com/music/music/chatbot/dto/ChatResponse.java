package com.music.music.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String type; // "rule" or "ai"
}

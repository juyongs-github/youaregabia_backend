package com.music.music.chatbot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatRequest {
    private String message;
    private List<MessageHistory> history;
}

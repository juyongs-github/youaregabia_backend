package com.music.music.chatbot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageHistory {
    private String role;    // "user" or "assistant"
    private String content;
}

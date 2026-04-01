package com.music.music.chatbot.dto;

import com.music.music.chatbot.entity.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageDto {

    private String role;
    private String content;
    private LocalDateTime createdAt;

    public ChatMessageDto(ChatMessage message) {
        this.role = message.getRole();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}

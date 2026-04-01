package com.music.music.chatbot.dto;

import com.music.music.chatbot.entity.ChatMessage;
import com.music.music.chatbot.entity.ChatSession;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ChatSessionDto {

    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastMessage; // 미리보기용

    public ChatSessionDto(ChatSession session) {
        this.sessionId = session.getSessionKey();
        this.createdAt = session.getCreatedAt();
        this.updatedAt = session.getUpdatedAt();

        List<ChatMessage> messages = session.getMessages();
        if (!messages.isEmpty()) {
            String last = messages.get(messages.size() - 1).getContent();
            this.lastMessage = last.length() > 50 ? last.substring(0, 50) + "..." : last;
        }
    }
}

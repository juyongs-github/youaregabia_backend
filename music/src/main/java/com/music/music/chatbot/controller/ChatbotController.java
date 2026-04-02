package com.music.music.chatbot.controller;

import com.music.music.chatbot.dto.ChatMessageDto;
import com.music.music.chatbot.dto.ChatRequest;
import com.music.music.chatbot.dto.ChatResponse;
import com.music.music.chatbot.dto.ChatSessionDto;
import com.music.music.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    // 메시지 전송
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal String email) {
        ChatResponse response = chatbotService.processMessage(request, email);
        return ResponseEntity.ok(response);
    }

    // 내 세션 목록 조회
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDto>> getSessions(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatbotService.getSessions(email));
    }

    // 특정 세션 메시지 조회
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable String sessionId,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(chatbotService.getMessages(sessionId, email));
    }

    // 세션 삭제
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal String email) {
        chatbotService.deleteSession(sessionId, email);
        return ResponseEntity.noContent().build();
    }
}

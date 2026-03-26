package com.music.music.chatbot.controller;

import com.music.music.chatbot.dto.ChatRequest;
import com.music.music.chatbot.dto.ChatResponse;
import com.music.music.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal String email) {
        ChatResponse response = chatbotService.processMessage(request, email);
        return ResponseEntity.ok(response);
    }
}

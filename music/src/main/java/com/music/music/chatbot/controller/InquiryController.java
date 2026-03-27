package com.music.music.chatbot.controller;

import com.music.music.chatbot.dto.InquiryRequest;
import com.music.music.chatbot.dto.InquiryResponseDto;
import com.music.music.chatbot.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    // 문의 접수
    @PostMapping
    public ResponseEntity<Void> sendInquiry(
            @RequestBody InquiryRequest request,
            @AuthenticationPrincipal String email) {
        inquiryService.sendInquiry(request, email);
        return ResponseEntity.ok().build();
    }

    // 내 문의 내역 조회
    @GetMapping("/my")
    public ResponseEntity<List<InquiryResponseDto>> getMyInquiries(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(email));
    }

    // 전체 문의 내역 조회 (관리자)
    @GetMapping
    public ResponseEntity<List<InquiryResponseDto>> getAllInquiries() {
        return ResponseEntity.ok(inquiryService.getAllInquiries());
    }
}

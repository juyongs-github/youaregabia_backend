package com.music.music.chatbot.dto;

import com.music.music.chatbot.entity.Inquiry;
import com.music.music.chatbot.entity.InquiryStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InquiryResponseDto {

    private final Long id;
    private final String type;
    private final String content;
    private final String status;
    private final LocalDateTime createdAt;
    private final String email;
    private final String phone;
    private final String userName;
    private final String userEmail;
    private final String answer;
    private final LocalDateTime answeredAt;

    public InquiryResponseDto(Inquiry inquiry) {
        this.id = inquiry.getId();
        this.type = inquiry.getType();
        this.content = inquiry.getContent();
        this.status = inquiry.getStatus() == InquiryStatus.PENDING ? "접수중" : "답변완료";
        this.createdAt = inquiry.getCreatedAt();
        this.email = inquiry.getEmail();
        this.phone = inquiry.getPhone();
        this.userName = inquiry.getUser() != null ? inquiry.getUser().getName() : null;
        this.userEmail = inquiry.getUser() != null ? inquiry.getUser().getEmail() : null;
        this.answer = inquiry.getAnswer();
        this.answeredAt = inquiry.getAnsweredAt();
    }
}

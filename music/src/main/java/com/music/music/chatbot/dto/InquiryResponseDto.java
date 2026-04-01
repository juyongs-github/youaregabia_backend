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

    public InquiryResponseDto(Inquiry inquiry) {
        this.id = inquiry.getId();
        this.type = inquiry.getType();
        this.content = inquiry.getContent();
        this.status = inquiry.getStatus() == InquiryStatus.PENDING ? "접수중" : "답변완료";
        this.createdAt = inquiry.getCreatedAt();
    }
}

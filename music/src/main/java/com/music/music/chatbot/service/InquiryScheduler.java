package com.music.music.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InquiryScheduler.class);

    private final InquiryService inquiryService;

    // 매 3분 마다 미발송 문의 일괄 발송
    @Scheduled(cron = "${inquiry.scheduler.cron}")
    public void sendPendingInquiries() {
        logger.info("[InquiryScheduler] 문의 일괄 발송 시작");
        inquiryService.sendPendingInquiries();
    }
}

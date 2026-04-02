package com.music.music.chatbot.service;

import com.music.music.chatbot.dto.InquiryRequest;
import com.music.music.chatbot.dto.InquiryResponseDto;
import com.music.music.chatbot.entity.Inquiry;
import com.music.music.chatbot.entity.InquiryStatus;
import com.music.music.chatbot.repository.InquiryRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final Logger logger = LoggerFactory.getLogger(InquiryService.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${admin.contact.email}")
    private String adminContactEmail;

    @Transactional
    public void sendInquiry(InquiryRequest request, String userEmail) {
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }
        String from = (userEmail != null) ? userEmail : request.getEmail();

        // 비로그인 사용자 이메일 형식 검증
        if (userEmail == null && (from == null || from.isBlank())) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        if (userEmail == null && !EMAIL_PATTERN.matcher(from).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        // 문의 내용 검증
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("문의 내용을 입력해주세요.");
        }
        if (request.getContent().length() > 1000) {
            throw new IllegalArgumentException("문의 내용은 1000자 이내로 입력해주세요.");
        }

        Inquiry inquiry = inquiryRepository.save(Inquiry.builder()
                .user(user)
                .type(request.getType())
                .content(request.getContent())
                .email(from)
                .build());

        // 즉시 이메일 발송 (실패 시 스케줄러가 재시도)
        if (mailSender != null) {
            try {
                sendEmail(inquiry);
                inquiry.markEmailSent();
                logger.info("[InquiryService] 문의 즉시 발송 완료 - 작성자: {}", from);
            } catch (Exception e) {
                logger.warn("[InquiryService] 즉시 발송 실패, 스케줄러 재시도 예정 - 작성자: {}, 오류: {}", from, e.getMessage());
            }
        } else {
            logger.info("[InquiryService] 문의 접수 완료 (메일 서버 미설정) - 작성자: {}", from);
        }
    }

    private void sendEmail(Inquiry inq) {
        String from = inq.getEmail() != null ? inq.getEmail() : "비회원";
        String body = "[GAP Music 문의]\n\n"
                + "유형: " + inq.getType() + "\n"
                + "작성자: " + from + "\n"
                + "접수일: " + inq.getCreatedAt() + "\n\n"
                + "내용:\n" + inq.getContent();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminContactEmail);
        message.setSubject("[GAP Music 문의] " + inq.getType() + " | " + from);
        message.setText(body);
        mailSender.send(message);
    }

    // 스케줄러에서 호출 — 미발송 문의 일괄 이메일 발송
    @Transactional
    public void sendPendingInquiries() {
        List<Inquiry> pending = inquiryRepository.findByEmailSentFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            logger.info("[InquiryService] 발송 대기 문의 없음");
            return;
        }

        if (mailSender == null) {
            logger.warn("[InquiryService] 메일 서버 미설정 - 발송 대기 문의 {}건 스킵", pending.size());
            return;
        }

        int successCount = 0;
        for (Inquiry inq : pending) {
            try {
                sendEmail(inq);
                inq.markEmailSent();
                successCount++;
            } catch (Exception e) {
                logger.error("[InquiryService] 문의 재발송 실패 (id={}): {}", inq.getId(), e.getMessage());
            }
        }
        logger.info("[InquiryService] 미발송 문의 재발송 완료 - {}건 / 전체 {}건", successCount, pending.size());
    }

    // 내 문의 내역 조회 (로그인 유저)
    @Transactional(readOnly = true)
    public List<InquiryResponseDto> getMyInquiries(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return inquiryRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(InquiryResponseDto::new)
                .collect(Collectors.toList());
    }

    // 전체 문의 내역 조회 (관리자)
    @Transactional(readOnly = true)
    public List<InquiryResponseDto> getAllInquiries() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(InquiryResponseDto::new)
                .collect(Collectors.toList());
    }

    // 문의 상태 변경 (관리자)
    @Transactional
    public void updateStatus(Long id, InquiryStatus status) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
        inquiry.updateStatus(status);
    }
}

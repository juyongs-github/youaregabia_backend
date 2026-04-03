package com.music.music.chatbot.service;

import com.music.music.chatbot.dto.InquiryRequest;
import com.music.music.chatbot.dto.InquiryResponseDto;
import com.music.music.chatbot.entity.Inquiry;
import com.music.music.chatbot.entity.InquiryStatus;
import com.music.music.chatbot.repository.InquiryRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class InquiryService {

    private static final Logger logger = LoggerFactory.getLogger(InquiryService.class);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0\\d{9,10}$");

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final DefaultMessageService smsService;

    @Value("${admin.contact.email}")
    private String adminContactEmailRaw; // 쉼표 구분 다중 이메일

    @Value("${coolsms.from.number}")
    private String smsFromNumber;

    public InquiryService(
            InquiryRepository inquiryRepository,
            UserRepository userRepository,
            @Value("${coolsms.api.key}") String coolSmsApiKey,
            @Value("${coolsms.api.secret}") String coolSmsApiSecret) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
        this.smsService = NurigoApp.INSTANCE.initialize(
                coolSmsApiKey, coolSmsApiSecret, "https://api.coolsms.co.kr");
    }

    @Transactional
    public void sendInquiry(InquiryRequest request, String userEmail) {
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        // 연락처: 챗봇에서 사용자가 직접 입력한 값만 사용 (로그인 여부 무관)
        String notifyEmail = request.getEmail();
        String notifyPhone = request.getPhone();

        // 이메일 또는 전화번호 중 하나 필수
        if (isBlank(notifyEmail) && isBlank(notifyPhone)) {
            throw new IllegalArgumentException("이메일 또는 휴대폰 번호를 입력해주세요.");
        }
        if (!isBlank(notifyEmail) && !EMAIL_PATTERN.matcher(notifyEmail).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
        if (!isBlank(notifyPhone)) {
            String normalizedPhone = notifyPhone.replaceAll("[^0-9]", "");
            if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
                throw new IllegalArgumentException("올바른 휴대폰 번호 형식이 아닙니다.");
            }
            notifyPhone = normalizedPhone;
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
                .email(notifyEmail)
                .phone(notifyPhone)
                .build());

        String contact = !isBlank(notifyEmail) ? notifyEmail : notifyPhone;

        // 1. 관리자 이메일 발송 (항상)
        if (mailSender != null) {
            try {
                sendAdminEmail(inquiry);
                inquiry.markEmailSent();
                logger.info("[InquiryService] 관리자 알림 발송 완료 - 작성자: {}", contact);
            } catch (Exception e) {
                logger.warn("[InquiryService] 관리자 이메일 발송 실패, 스케줄러 재시도 예정 - 작성자: {}, 오류: {}", contact, e.getMessage());
            }
        } else {
            logger.info("[InquiryService] 문의 접수 완료 (메일 서버 미설정) - 작성자: {}", contact);
        }

        // 2. 사용자 이메일 알림 (이메일 있을 때)
        if (!isBlank(notifyEmail) && mailSender != null) {
            try {
                sendUserEmail(inquiry);
                logger.info("[InquiryService] 사용자 이메일 접수 알림 발송 - {}", notifyEmail);
            } catch (Exception e) {
                logger.warn("[InquiryService] 사용자 이메일 발송 실패 - {}: {}", notifyEmail, e.getMessage());
            }
        }

        // 3. 사용자 SMS 알림 (전화번호 있을 때)
        if (!isBlank(notifyPhone)) {
            try {
                sendUserSms(inquiry);
                logger.info("[InquiryService] 사용자 SMS 접수 알림 발송 - {}", notifyPhone);
            } catch (Exception e) {
                logger.warn("[InquiryService] 사용자 SMS 발송 실패 - {}: {}", notifyPhone, e.getMessage());
            }
        }
    }

    // 관리자에게 문의 접수 알림 이메일
    private void sendAdminEmail(Inquiry inq) {
        String from = inq.getEmail() != null ? inq.getEmail()
                : inq.getPhone() != null ? inq.getPhone() : "비회원";
        String body = "[GAP Music 문의]\n\n"
                + "유형: " + inq.getType() + "\n"
                + "작성자: " + from + "\n"
                + "접수일: " + inq.getCreatedAt() + "\n\n"
                + "내용:\n" + inq.getContent();

        String[] recipients = Arrays.stream(adminContactEmailRaw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipients);
        message.setSubject("[GAP Music 문의] " + inq.getType() + " | " + from);
        message.setText(body);
        mailSender.send(message);
    }

    // 사용자에게 문의 접수 확인 이메일
    private void sendUserEmail(Inquiry inq) {
        String body = "[GAP Music] 문의가 접수되었습니다.\n\n"
                + "유형: " + inq.getType() + "\n"
                + "접수일: " + inq.getCreatedAt() + "\n\n"
                + "내용:\n" + inq.getContent() + "\n\n"
                + "답변은 영업일 기준 1~3일 내로 이메일로 안내드립니다.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(inq.getEmail());
        message.setSubject("[GAP Music] 문의가 접수되었습니다.");
        message.setText(body);
        mailSender.send(message);
    }

    // 사용자에게 문의 접수 확인 SMS
    private void sendUserSms(Inquiry inq) {
        Message message = new Message();
        message.setFrom(smsFromNumber);
        message.setTo(inq.getPhone());
        String preview = inq.getContent().length() > 30
                ? inq.getContent().substring(0, 30) + "…" : inq.getContent();
        message.setText("문의가 접수되었습니다.\n유형: " + inq.getType()
                + "\n내용: " + preview
                + "\n빠른 시일 내에 답변드리겠습니다.");
        smsService.sendOne(new SingleMessageSendingRequest(message));
    }

    // 사용자에게 답변 완료 알림 이메일
    private void sendAnswerNotificationEmail(Inquiry inq) {
        String body = "[GAP Music] 문의하신 내용에 답변이 등록되었습니다.\n\n"
                + "유형: " + inq.getType() + "\n"
                + "문의 내용:\n" + inq.getContent() + "\n\n"
                + "─────────────────────\n"
                + "관리자 답변:\n" + inq.getAnswer() + "\n"
                + "─────────────────────\n\n"
                + "추가 문의사항이 있으시면 다시 문의해 주세요.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(inq.getEmail());
        message.setSubject("[GAP Music] 문의 답변이 등록되었습니다.");
        message.setText(body);
        mailSender.send(message);
    }

    // 사용자에게 답변 완료 알림 SMS
    private void sendAnswerNotificationSms(Inquiry inq) {
        String preview = inq.getAnswer().length() > 40
                ? inq.getAnswer().substring(0, 40) + "…" : inq.getAnswer();
        Message message = new Message();
        message.setFrom(smsFromNumber);
        message.setTo(inq.getPhone());
        message.setText("[GAP Music] 문의 답변이 등록되었습니다.\n"
                + "유형: " + inq.getType() + "\n"
                + "답변: " + preview);
        smsService.sendOne(new SingleMessageSendingRequest(message));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
                sendAdminEmail(inq);
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

    // 답변 등록/수정 (관리자)
    @Transactional
    public InquiryResponseDto saveAnswer(Long id, String answer) {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("답변 내용을 입력해주세요.");
        }
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
        inquiry.saveAnswer(answer.trim());

        // 이메일 알림
        if (!isBlank(inquiry.getEmail()) && mailSender != null) {
            try {
                sendAnswerNotificationEmail(inquiry);
                logger.info("[InquiryService] 답변 알림 이메일 발송 - {}", inquiry.getEmail());
            } catch (Exception e) {
                logger.warn("[InquiryService] 답변 알림 이메일 발송 실패 - {}: {}", inquiry.getEmail(), e.getMessage());
            }
        }

        // SMS 알림
        if (!isBlank(inquiry.getPhone())) {
            try {
                sendAnswerNotificationSms(inquiry);
                logger.info("[InquiryService] 답변 알림 SMS 발송 - {}", inquiry.getPhone());
            } catch (Exception e) {
                logger.warn("[InquiryService] 답변 알림 SMS 발송 실패 - {}: {}", inquiry.getPhone(), e.getMessage());
            }
        }

        return new InquiryResponseDto(inquiry);
    }

    // 답변 삭제 (관리자)
    @Transactional
    public InquiryResponseDto deleteAnswer(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
        inquiry.deleteAnswer();
        return new InquiryResponseDto(inquiry);
    }

    // 문의 삭제 (관리자)
    @Transactional
    public void deleteInquiry(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));
        inquiryRepository.delete(inquiry);
    }
}

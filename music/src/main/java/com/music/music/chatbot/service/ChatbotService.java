package com.music.music.chatbot.service;

import com.music.music.chatbot.dto.ChatRequest;
import com.music.music.chatbot.dto.ChatResponse;
import com.music.music.chatbot.dto.ChatSessionDto;
import com.music.music.chatbot.dto.ChatMessageDto;
import com.music.music.chatbot.entity.ChatMessage;
import com.music.music.chatbot.entity.ChatSession;
import com.music.music.chatbot.repository.ChatMessageRepository;
import com.music.music.chatbot.repository.ChatSessionRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    private final RuleBasedService ruleBasedService;
    private final OpenAiService openAiService;
    private final ItunesService itunesService;
    private final UserRepository userRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    private static final int HISTORY_LIMIT = 20; // 프롬프트에 포함할 최근 메시지 수

    private static final int MAX_MESSAGE_LENGTH = 300;

    private static final List<String> BLOCKED_WORDS = List.of(
            "씨발", "시발", "개새끼", "ㅅㅂ", "ㅂㅅ", "병신", "지랄", "닥쳐", "죽어", "꺼져",
            "fuck", "shit", "bitch", "asshole", "bastard"
    );

    private static final List<String> GUIDE_KEYWORDS = List.of(
            "어떻게", "방법", "사용법", "기능", "도움", "가이드", "알려줘", "뭐야", "무엇",
            "설명", "안내", "모르", "도와줘", "어디서", "어디에"
    );

    private static final List<String> FEATURE_KEYWORDS = List.of(
            "플레이리스트", "랭킹", "게시판", "커뮤니티",
            "좋아요", "포인트", "등급", "굿즈", "검색", "콜라보",
            "추천 기능", "추천 방법", "크리틱", "평론", "퀴즈", "게임", "미니게임",
            "주문", "배송", "마이페이지", "프로필", "공유", "공동",
            "비밀번호", "계정", "재생", "오류", "버그",
            "문의", "챗봇", "신고", "불편", "도움말", "어떤 기능", "무슨 기능", "뭐 할 수"
    );

    @Transactional
    public ChatResponse processMessage(ChatRequest request, String email) {
        String message = request.getMessage();

        // 0. 입력 검증
        if (message == null || message.isBlank()) {
            return new ChatResponse("질문을 입력해주세요 🎵", "rule", request.getSessionId());
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return new ChatResponse("질문이 너무 깁니다. " + MAX_MESSAGE_LENGTH + "자 이내로 입력해주세요.", "rule", request.getSessionId());
        }
        if (isBlocked(message)) {
            return new ChatResponse("올바른 언어로 질문해주세요 😊", "rule", request.getSessionId());
        }

        // 1. 규칙 기반 체크
        if (isGuideIntent(message)) {
            String ruleResponse = ruleBasedService.getResponse(message);
            if (ruleResponse != null) {
                String sessionId = saveToSession(request.getSessionId(), email, message, ruleResponse);
                return new ChatResponse(ruleResponse, "rule", sessionId);
            }
            // 가이드 의도는 감지됐지만 매칭 규칙 없음 → AI가 서비스 기능을 모르므로 메뉴로 안내
            String fallback = "챗봇 하단 메뉴에서 원하는 기능을 선택하거나, 더 구체적으로 질문해주세요 🎵\n예) '플레이리스트 어떻게 만들어?', '포인트 어떻게 쌓아?'";
            String sessionId = saveToSession(request.getSessionId(), email, message, fallback);
            return new ChatResponse(fallback, "rule", sessionId);
        }

        // 2. 사용자 나이 조회
        Integer age = null;
        User user = null;
        if (email != null) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                user = userOpt.get();
                if (user.getBirthDate() != null) {
                    age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
                }
            }
        }

        // 3. 세션에서 히스토리 로드
        ChatSession session = getOrCreateSession(request.getSessionId(), user);
        List<ChatMessage> history = chatMessageRepository.findRecentMessages(session.getId(), PageRequest.of(0, HISTORY_LIMIT));
        Collections.reverse(history); // DESC로 가져온 것을 시간순으로 복원

        // 4. 이전 세션 + 현재 세션에서 이미 추천한 곡명 추출 (중복 방지)
        List<String> previousSongTitles = getPreviousRecommendedSongs(user, session.getId(), history);

        // 5. AI 호출
        String aiResponse = openAiService.getResponse(message, history, age, previousSongTitles);

        // 5-1. iTunes로 추천 곡 검증 — 40% 미만 확인되면 1회 재시도
        List<String[]> extractedSongs = itunesService.extractSongs(aiResponse);
        if (!extractedSongs.isEmpty()) {
            double rate = itunesService.verifyRate(extractedSongs);
            if (rate < 0.4) {
                logger.info("[ChatbotService] iTunes 검증률 낮음({}%), AI 재호출", String.format("%.0f", rate * 100));
                aiResponse = openAiService.getResponse(message, history, age, previousSongTitles);
            }
        }

        // 6. 메시지 저장
        chatMessageRepository.save(ChatMessage.builder()
                .session(session)
                .role("user")
                .content(message)
                .build());
        chatMessageRepository.save(ChatMessage.builder()
                .session(session)
                .role("assistant")
                .content(aiResponse)
                .build());
        session.touch();

        return new ChatResponse(aiResponse, "ai", session.getSessionKey());
    }

    // 이전 세션 + 현재 세션 히스토리에서 이미 추천한 곡명 추출 (중복 방지)
    private List<String> getPreviousRecommendedSongs(User user, Long currentSessionId, List<ChatMessage> currentHistory) {
        List<String> songTitles = new ArrayList<>();

        // 현재 세션 히스토리에서 추출
        if (currentHistory != null) {
            currentHistory.stream()
                    .filter(m -> "assistant".equals(m.getRole()))
                    .flatMap(m -> itunesService.extractSongTitles(m.getContent()).stream())
                    .forEach(songTitles::add);
        }

        // 다른 세션 최근 5개에서 추출
        if (user != null) {
            chatMessageRepository
                    .findRecentAssistantMessagesFromOtherSessions(user.getId(), currentSessionId, PageRequest.of(0, 5))
                    .stream()
                    .flatMap(m -> itunesService.extractSongTitles(m.getContent()).stream())
                    .forEach(songTitles::add);
        }

        return songTitles.stream().distinct().collect(Collectors.toList());
    }

    // 세션 목록 조회
    @Transactional(readOnly = true)
    public List<ChatSessionDto> getSessions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(ChatSessionDto::new)
                .collect(Collectors.toList());
    }

    // 세션 메시지 조회
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(String sessionId, String email) {
        ChatSession session = chatSessionRepository.findBySessionKey(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        validateOwner(session, email);
        return session.getMessages().stream()
                .map(ChatMessageDto::new)
                .collect(Collectors.toList());
    }

    // 세션 삭제
    @Transactional
    public void deleteSession(String sessionId, String email) {
        ChatSession session = chatSessionRepository.findBySessionKey(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        validateOwner(session, email);
        chatSessionRepository.delete(session);
    }

    // --- private helpers ---

    private ChatSession getOrCreateSession(String sessionId, User user) {
        if (sessionId != null) {
            Optional<ChatSession> existing = chatSessionRepository.findBySessionKey(sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return chatSessionRepository.save(ChatSession.builder()
                .sessionKey(UUID.randomUUID().toString())
                .user(user)
                .build());
    }

    private String saveToSession(String sessionId, String email, String userMsg, String assistantMsg) {
        User user = null;
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        ChatSession session = getOrCreateSession(sessionId, user);
        chatMessageRepository.save(ChatMessage.builder().session(session).role("user").content(userMsg).build());
        chatMessageRepository.save(ChatMessage.builder().session(session).role("assistant").content(assistantMsg).build());
        session.touch();
        return session.getSessionKey();
    }

    private void validateOwner(ChatSession session, String email) {
        if (session.getUser() == null) return;
        if (!session.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
    }

    private boolean isBlocked(String message) {
        String lower = message.toLowerCase();
        return BLOCKED_WORDS.stream().anyMatch(lower::contains);
    }

    private boolean isGuideIntent(String message) {
        String lower = message.toLowerCase();
        boolean hasGuideKeyword = GUIDE_KEYWORDS.stream().anyMatch(lower::contains);
        boolean hasFeatureKeyword = FEATURE_KEYWORDS.stream().anyMatch(lower::contains);
        return hasGuideKeyword && hasFeatureKeyword;
    }
}

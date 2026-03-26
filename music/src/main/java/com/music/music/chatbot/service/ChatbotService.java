package com.music.music.chatbot.service;

import com.music.music.chatbot.dto.ChatRequest;
import com.music.music.chatbot.dto.ChatResponse;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final RuleBasedService ruleBasedService;
    private final OpenAiService openAiService;
    private final UserRepository userRepository;

    private static final List<String> GUIDE_KEYWORDS = List.of(
            "어떻게", "방법", "사용법", "기능", "도움", "가이드", "알려줘", "뭐야", "무엇",
            "설명", "안내", "모르", "도와줘", "어디서", "어디에"
    );

    private static final List<String> FEATURE_KEYWORDS = List.of(
            "플레이리스트", "회원가입", "로그인", "랭킹", "게시판", "커뮤니티",
            "좋아요", "포인트", "등급", "굿즈", "검색", "콜라보"
    );

    public ChatResponse processMessage(ChatRequest request, String email) {
        String message = request.getMessage();

        if (isGuideIntent(message)) {
            String ruleResponse = ruleBasedService.getResponse(message);
            if (ruleResponse != null) {
                return new ChatResponse(ruleResponse, "rule");
            }
        }

        Integer age = null;
        if (email != null) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent() && userOpt.get().getBirthDate() != null) {
                age = Period.between(userOpt.get().getBirthDate(), LocalDate.now()).getYears();
            }
        }

        String aiResponse = openAiService.getResponse(message, request.getHistory(), age);
        return new ChatResponse(aiResponse, "ai");
    }

    private boolean isGuideIntent(String message) {
        String lower = message.toLowerCase();
        boolean hasGuideKeyword = GUIDE_KEYWORDS.stream().anyMatch(lower::contains);
        boolean hasFeatureKeyword = FEATURE_KEYWORDS.stream().anyMatch(lower::contains);
        return hasGuideKeyword && hasFeatureKeyword;
    }
}

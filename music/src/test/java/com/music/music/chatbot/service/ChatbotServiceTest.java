package com.music.music.chatbot.service;

import com.music.music.chatbot.dto.ChatRequest;
import com.music.music.chatbot.dto.ChatResponse;
import com.music.music.chatbot.entity.ChatMessage;
import com.music.music.chatbot.entity.ChatSession;
import com.music.music.chatbot.repository.ChatMessageRepository;
import com.music.music.chatbot.repository.ChatSessionRepository;
import com.music.music.recommendation.service.RecommendationOrchestrator;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private RuleBasedService ruleBasedService;

    @Mock
    private OpenAiService openAiService;

    @Mock
    private ItunesService itunesService;

    @Mock
    private ChartService chartService;

    @Mock
    private RecommendationOrchestrator recommendationOrchestrator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    void featureKeywordOnlyQuestionUsesRuleResponse() {
        ChatRequest request = request("플레이리스트", null);

        when(ruleBasedService.getResponse("플레이리스트")).thenReturn("플레이리스트 안내");

        ChatSession newSession = ChatSession.builder()
                .sessionKey("new-session")
                .build();
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(newSession);

        ChatResponse response = chatbotService.processMessage(request, null);

        assertThat(response.getType()).isEqualTo("rule");
        assertThat(response.getMessage()).isEqualTo("플레이리스트 안내");
        assertThat(response.getSessionId()).isEqualTo("new-session");
        verify(openAiService, never()).getResponse(any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void followUpQuestionKeepsContextWithoutRequestingFreshRecommendations() {
        ChatRequest request = request("이 노래는 언제 나왔어?", "session-1");

        ChatSession session = ChatSession.builder()
                .sessionKey("session-1")
                .build();
        ReflectionTestUtils.setField(session, "id", 1L);

        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role("assistant")
                .content("\"봄날\" - BTS")
                .build();

        when(chatSessionRepository.findBySessionKey("session-1")).thenReturn(Optional.of(session));
        when(chatMessageRepository.findRecentMessages(eq(1L), any(Pageable.class))).thenReturn(List.of(assistantMessage));
        when(chartService.getChart()).thenReturn(List.of("\"TOO BAD\" - G-DRAGON"));
        when(openAiService.getResponse(any(), any(), any(), any(), any(), any(), eq(true))).thenReturn("후속 답변");

        ChatResponse response = chatbotService.processMessage(request, null);

        assertThat(response.getMessage()).isEqualTo("후속 답변");
        verify(openAiService).getResponse(
                eq("이 노래는 언제 나왔어?"),
                any(),
                eq(null),
                eq(null),
                any(),
                eq(null),
                eq(true));
        verify(itunesService, never()).verifyRate(any());
    }

    @Test
    void anonymousSessionIsClaimedWhenUserContinuesAfterLogin() {
        ChatRequest request = request("잔잔한 노래 추천해줘", "session-1");

        User user = User.builder()
                .email("user@test.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        ChatSession session = ChatSession.builder()
                .sessionKey("session-1")
                .build();
        ReflectionTestUtils.setField(session, "id", 1L);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(chatSessionRepository.findBySessionKey("session-1")).thenReturn(Optional.of(session));
        when(chatMessageRepository.findRecentMessages(eq(1L), any(Pageable.class))).thenReturn(List.of());
        when(chatMessageRepository.findRecentAssistantMessagesFromOtherSessions(eq(10L), eq(1L), any(Pageable.class)))
                .thenReturn(List.of());
        when(chartService.getChart()).thenReturn(List.of());
        when(openAiService.getResponse(any(), any(), any(), any(), any(), any(), eq(false))).thenReturn("\"밤편지\" - 아이유");
        when(itunesService.extractSongs("\"밤편지\" - 아이유")).thenReturn(Collections.singletonList(new String[]{"밤편지", "아이유"}));
        when(itunesService.verifyRate(any())).thenReturn(1.0);

        ChatResponse response = chatbotService.processMessage(request, "user@test.com");

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(session.getUser()).isEqualTo(user);
    }

    @Test
    void cannotContinueAnotherUsersSession() {
        ChatRequest request = request("추천해줘", "session-1");

        User owner = User.builder()
                .email("owner@test.com")
                .build();

        ChatSession session = ChatSession.builder()
                .sessionKey("session-1")
                .user(owner)
                .build();

        when(chatSessionRepository.findBySessionKey("session-1")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> chatbotService.processMessage(request, "other@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이 대화 세션에 접근할 수 없습니다.");
    }

    private ChatRequest request(String message, String sessionId) {
        ChatRequest request = new ChatRequest();
        ReflectionTestUtils.setField(request, "message", message);
        ReflectionTestUtils.setField(request, "sessionId", sessionId);
        return request;
    }
}

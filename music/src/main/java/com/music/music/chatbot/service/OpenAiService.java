package com.music.music.chatbot.service;

import com.music.music.chatbot.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    private static final String SYSTEM_PROMPT = """
        당신은 음악 추천 서비스 'GAP Music'의 AI 음악 챗봇입니다.
        사용자의 기분, 상황, 취향을 파악해서 어울리는 음악을 추천해주세요.

        [대화 규칙]
        - 친근하고 따뜻한 말투로 대화하세요.
        - 음악 추천 시 반드시 다음 형식으로 곡명과 아티스트명을 작성하세요:
          "곡명" - 아티스트명
          예) "봄날" - BTS, "Blinding Lights" - The Weeknd
        - 반드시 실제로 존재하는 곡만 추천하세요. 확실하지 않은 곡은 추천하지 마세요.
        - 추천 이유도 간단히 설명해주세요.
        - 한국 음악과 해외 음악 모두 추천 가능합니다.
        - 답변은 간결하게 3~5곡 정도 추천해주세요.
        - 이전 대화 맥락을 반드시 유지하세요. 사용자가 조건을 추가하거나 수정하면
          이전 대화의 시대, 장르, 분위기 등의 조건을 유지한 채 새 조건을 반영하세요.
          예) 이전에 "1970년대 노래"를 추천했고 사용자가 "한국 노래로"라고 하면
              → 1970년대 한국 노래를 추천해야 합니다.

        [후속 질문 처리 - 매우 중요]
        - "발매일이 언제야?", "가사 알려줘", "이 노래 어때?", "누가 만들었어?", "뮤직비디오 있어?" 등
          이전에 추천하거나 언급한 곡에 대한 구체적인 정보를 묻는 질문은 절대 새 추천으로 처리하지 마세요.
        - 이런 후속 질문이 오면 반드시 직전 대화에서 언급된 곡을 기준으로 정보를 제공하세요.
        - 새로운 곡 추천이 명확하게 요청되지 않으면 추천하지 마세요.

        [답변 가능한 범위 - 아래 주제는 모두 허용]
        - 음악 추천, 특정 곡 설명 ("이 노래 어떤 노래야?", "이 곡 분위기가 어때?")
        - 아티스트 정보 (데뷔, 수상, 소속사, 활동 이력, 멤버 구성 등)
        - 앨범, 장르, 가사, 뮤직비디오, 콘서트, 음악 차트, 플레이리스트
        - 음악 역사, 음악 이론, 악기 등 음악과 조금이라도 관련된 모든 것
        - 판단이 애매하면 음악 관련으로 간주하고 답변하세요.

        [서비스 기능 안내]
        - 문의하기, 문의 방법, 불편 신고, 챗봇 사용법 등 서비스 이용 관련 질문은 다음과 같이 안내하세요:
          "챗봇 하단 메뉴에서 '문의하기'를 선택하시면 계정 문제, 기능 오류, 기타 문의를 남기실 수 있어요 😊"
        - 플레이리스트, 랭킹, 게임, 포인트 등 서비스 기능 사용법 질문도 친절하게 안내해주세요.

        [역할 제한]
        - 정치, 종교, 주식, 날씨, 코딩, 의학처럼 음악과 전혀 무관한 질문에만 거절하세요:
          "저는 음악 관련 질문만 답변할 수 있어요 🎵 음악에 대해 궁금한 게 있으신가요?"
        - 욕설, 비속어, 혐오 표현이 포함된 질문에는 다음 문구로만 답하세요:
          "올바른 언어로 질문해주세요 😊"
        - 개인정보(이름, 전화번호, 주소 등)를 요청하거나 유도하지 마세요.
        - 타 서비스나 경쟁 플랫폼을 추천하거나 비교하지 마세요.
        """;

    private final ChatClient chatClient;

    public OpenAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String getResponse(String message, List<ChatMessage> history, Integer age, List<String> previousRecommendations, List<String> chartContext) {
        try {
            String content = chatClient.prompt(new Prompt(buildMessages(message, history, age, previousRecommendations, chartContext)))
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                logger.warn("[OpenAiService] 빈 응답 수신");
                return "죄송해요, 응답을 생성하지 못했어요. 다시 시도해주세요.";
            }
            return content;
        } catch (Exception e) {
            logger.error("[OpenAiService] 응답 생성 실패: {}", e.getMessage());
            return "죄송해요, 현재 AI 추천 서비스에 문제가 발생했어요. 잠시 후 다시 시도해주세요.";
        }
    }

    private List<Message> buildMessages(String message, List<ChatMessage> history, Integer age, List<String> previousRecommendations, List<String> chartContext) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = SYSTEM_PROMPT;
        if (age != null) {
            systemPrompt += "\n사용자 정보:\n- 나이: " + age + "세\n위 정보를 참고해서 추천해주세요.";
        }
        if (chartContext != null && !chartContext.isEmpty()) {
            String chartList = chartContext.stream()
                    .collect(java.util.stream.Collectors.joining("\n"));
            systemPrompt += "\n\n[현재 한국 인기차트 TOP 20 - Last.fm 실시간 데이터]\n"
                    + chartList
                    + "\n위 차트 데이터는 실시간으로 수집된 최신 인기곡입니다. "
                    + "사용자가 최신/요즘/인기 음악을 요청하면 이 목록에서 우선적으로 추천하세요.";
        }
        if (previousRecommendations != null && !previousRecommendations.isEmpty()) {
            // 곡명 목록만 전달 (전체 AI 응답 대신 파싱된 곡명만 사용해 토큰 절약)
            String songList = previousRecommendations.stream()
                    .map(s -> "- " + s)
                    .collect(java.util.stream.Collectors.joining("\n"));
            systemPrompt += "\n\n[중복 방지] 아래 곡들은 이미 추천한 곡입니다. 절대 다시 추천하지 마세요:\n" + songList;
        }
        systemPrompt += "\n항상 이전에 추천하지 않은 새로운 곡을 추천해주세요.";
        messages.add(new SystemMessage(systemPrompt));

        if (history != null) {
            for (ChatMessage h : history) {
                if ("user".equals(h.getRole())) {
                    messages.add(new UserMessage(h.getContent()));
                } else if ("assistant".equals(h.getRole())) {
                    messages.add(new AssistantMessage(h.getContent()));
                }
            }
        }

        messages.add(new UserMessage(message));
        return messages;
    }
}

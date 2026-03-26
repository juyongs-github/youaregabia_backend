package com.music.music.chatbot.service;

import com.music.music.chatbot.dto.MessageHistory;
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
            당신은 음악 추천 서비스의 AI 음악 추천 챗봇입니다.
            사용자의 기분, 상황, 취향을 파악해서 어울리는 음악을 추천해주세요.

            - 친근하고 따뜻한 말투로 대화하세요.
            - 음악 추천 시 곡명과 아티스트명을 함께 알려주세요.
            - 추천 이유도 간단히 설명해주세요.
            - 한국 음악과 해외 음악 모두 추천 가능합니다.
            - 답변은 간결하게 3~5곡 정도 추천해주세요.
            """;

    private final ChatClient chatClient;

    public OpenAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String getResponse(String message, List<MessageHistory> history, Integer age) {
        try {
            List<Message> messages = new ArrayList<>();

            String systemPrompt = SYSTEM_PROMPT;
            if (age != null) {
                systemPrompt += "\n사용자 정보:\n- 나이: " + age + "세\n위 정보를 참고해서 추천해주세요.";
            }
            messages.add(new SystemMessage(systemPrompt));

            if (history != null) {
                for (MessageHistory h : history) {
                    if ("user".equals(h.getRole())) {
                        messages.add(new UserMessage(h.getContent()));
                    } else if ("assistant".equals(h.getRole())) {
                        messages.add(new AssistantMessage(h.getContent()));
                    }
                }
            }

            messages.add(new UserMessage(message));

            return chatClient.prompt(new Prompt(messages))
                    .call()
                    .content();

        } catch (Exception e) {
            logger.error("[ChatbotService] 응답 생성 실패: {}", e.getMessage());
            return "죄송해요, 현재 AI 추천 서비스에 문제가 발생했어요. 잠시 후 다시 시도해주세요.";
        }
    }
}

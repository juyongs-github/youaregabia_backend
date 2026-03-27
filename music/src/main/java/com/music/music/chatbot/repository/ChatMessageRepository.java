package com.music.music.chatbot.repository;

import com.music.music.chatbot.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 최근 N개 메시지만 가져오기 (DESC로 가져온 뒤 서비스에서 reverse)
    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("sessionId") Long sessionId, Pageable pageable);

    // 다른 세션의 최근 AI 응답 (cross-session 중복 방지용)
    @Query("SELECT m FROM ChatMessage m WHERE m.session.user.id = :userId AND m.session.id != :currentSessionId AND m.role = 'assistant' ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentAssistantMessagesFromOtherSessions(@Param("userId") Long userId, @Param("currentSessionId") Long currentSessionId, Pageable pageable);
}

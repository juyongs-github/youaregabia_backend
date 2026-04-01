package com.music.music.chatbot.repository;

import com.music.music.chatbot.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Inquiry> findAllByOrderByCreatedAtDesc();

    List<Inquiry> findByEmailSentFalseOrderByCreatedAtAsc();
}

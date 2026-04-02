package com.music.music.chatbot.entity;

import com.music.music.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String email; // 비로그인 사용자 이메일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.PENDING;

    @Builder.Default
    private boolean emailSent = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void markEmailSent() {
        this.emailSent = true;
    }

    public void updateStatus(InquiryStatus status) {
        this.status = status;
    }
}

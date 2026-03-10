package com.music.music.notification.entity;

import java.time.LocalDateTime;

import com.music.music.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 알림 받는 사람
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User receiver;

  // 알림 메시지
  @Column(nullable = false, length = 300)
  private String message;

  // 관련 게시글 ID (클릭 시 이동)
  @Column
  private Long boardId;

  // 읽음 여부
  @Column(nullable = false)
  @Builder.Default
  private boolean isRead = false;

  @Column(nullable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  public void markAsRead() {
    this.isRead = true;
  }
}

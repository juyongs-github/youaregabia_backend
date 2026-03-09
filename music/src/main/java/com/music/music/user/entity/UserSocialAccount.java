package com.music.music.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_social_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_social_provider", columnNames = { "provider", "provider_id" })
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 어떤 유저의 소셜 계정인지
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // GOOGLE / NAVER / KAKAO
  @Column(nullable = false, length = 20)
  private String provider;

  // 소셜에서 내려주는 고유 ID
  @Column(name = "provider_id", nullable = false, length = 255)
  private String providerId;

  @Column(name = "connected_at", nullable = false)
  private LocalDateTime connectedAt;

  @Column(name = "disconnected_at")
  private LocalDateTime disconnectedAt;

  @PrePersist
  protected void onCreate() {
    this.connectedAt = LocalDateTime.now();
  }
}

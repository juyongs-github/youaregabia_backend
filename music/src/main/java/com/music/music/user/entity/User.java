package com.music.music.user.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "users")
@Table(name = "users",
    // [ADD] DB 레벨 unique 강제(레이스 컨디션 방지)
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_ci", columnNames = "ci")
    })
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // 휴대폰 번호
  @Column(name = "phone_number", nullable = false, length = 20)
  private String phoneNumber;

  // 이름
  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  // 이메일 (소셜 대비 NULL 허용)
  @Column(length = 100)
  private String email;

  @Column(length = 255)
  private String address;

  // CI (본인인증 고유값)
  @Column(name = "ci", nullable = false, length = 255, unique = true)
  private String ci;

  @Column(name = "img_url", length = 255)
  private String imgUrl;

  // 로컬 로그인용 (소셜은 NULL)
  @Column(length = 255)
  private String password;

  // 소셜 계정 목록
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserSocialAccount> socialAccounts = new ArrayList<>();

  // 상태값 (1: 활성, 0: 비활성 등)
  @Column
  private Integer state;

  // 권한 (USER / CRITIC / ADMIN)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  // === JPA 라이프사이클 ===
  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
    if (this.state == null) {
      this.state = 1;
    }
    if (this.role == null) {
      this.role = Role.USER;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public void setImgUrl(String imgUrl) {
    this.imgUrl = imgUrl;
  }

  public void setPassword(String password) {
    this.password = password;
  }
<<<<<<< HEAD

  public void setRole(Role role) {
    this.role = role;
  }
}
=======
}
>>>>>>> origin/feature/board-after-gitignore

package com.music.music.board.entity;

import java.util.ArrayList;
import java.util.List;

import com.music.music.board.common.entity.BaseEntity;
import com.music.music.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "replies")
@Builder
@Table(name = "board")
public class Board extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long boardId;

   // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 게시글 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoardType boardType;
    
    // 게시글 장르
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoardGenre boardGenre;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false)
  private String content;

  @Column(nullable = false)
  private int viewCount = 0;

    // Soft Delete
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<Reply> replies = new ArrayList<>();

    public void delete() {
        this.deleted = true;
    }

    public void update(String title, String content, BoardGenre boardGenre) {
    this.title = title;
    this.content = content;
    this.boardGenre = boardGenre;
    }
    public void increaseViewCount() {
    this.viewCount++;
  }
}

package com.music.music.board.entity;

import java.util.ArrayList;
import java.util.List;

import com.music.music.board.common.entity.BaseEntity;
import com.music.music.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString(exclude = "replyLikes")
@Entity
public class Reply extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long replyId;

  // 어떤 게시글의 댓글인지
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  // 댓글 작성자
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 500)
  private String content;

  @Column(nullable = false)
  @Builder.Default
  private int likeCount = 0;

    // 대댓글 부모 참조 (null이면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private Reply parentReply;

    // Soft Delete
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "reply", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<ReplyLike> replyLikes = new ArrayList<>();

  public void updateContent(String content) {
    this.content = content;
    }
    
    public void delete() {
    this.deleted = true;
    }
}

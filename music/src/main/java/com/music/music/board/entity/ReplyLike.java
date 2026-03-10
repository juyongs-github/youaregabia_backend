package com.music.music.board.entity;

<<<<<<< HEAD
import com.music.music.common.entity.BaseEntity;
import com.music.music.user.entitiy.User;
=======
import com.music.music.board.common.entity.BaseEntity;
import com.music.music.user.entity.User;
>>>>>>> origin/feature/jylee_2

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
@Entity
public class ReplyLike extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long replyLikeId;

  // 어떤 댓글에 대한 좋아요인지
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reply_id", nullable = false)
  private Reply reply;

  // 누가 눌렀는지
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
}

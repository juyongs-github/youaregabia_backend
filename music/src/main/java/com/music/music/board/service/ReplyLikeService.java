package com.music.music.board.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.board.entity.Reply;
import com.music.music.board.entity.ReplyLike;
import com.music.music.board.repository.ReplyLikeRepository;
import com.music.music.board.repository.ReplyRepository;
<<<<<<< HEAD
import com.music.music.user.entitiy.User;
=======
import com.music.music.user.entity.User;
>>>>>>> origin/feature/jylee_2
import com.music.music.user.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@Transactional
@AllArgsConstructor
public class ReplyLikeService {
  private final ReplyRepository replyRepository;
  private final ReplyLikeRepository replyLikeRepository;
  private final UserRepository userRepository;

<<<<<<< HEAD
        public long toggleLike(Long replyId, String email) {
=======
  public long toggleLike(Long replyId, String email) {
>>>>>>> origin/feature/jylee_2

    Reply reply = replyRepository.findById(replyId)
        .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

<<<<<<< HEAD
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

                // 좋아요 이미 눌렀는지 확인
        boolean alreadyLiked =
                replyLikeRepository.existsByReply_ReplyIdAndUser_Email(replyId, email);

                // 이미 눌렀으면 좋아요를 취소
        if (alreadyLiked) {
            replyLikeRepository.deleteByReply_ReplyIdAndUser_Email(replyId, email);
        } else {
            replyLikeRepository.save(
                    ReplyLike.builder()
                            .reply(reply)
                            .user(user)
                            .build()
            );
        }

        return replyLikeRepository.countByReply_ReplyId(replyId);
=======
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

    // 좋아요 이미 눌렀는지 확인
    boolean alreadyLiked = replyLikeRepository.existsByReply_ReplyIdAndUser_Email(replyId, email);

    // 이미 눌렀으면 좋아요를 취소
    if (alreadyLiked) {
      replyLikeRepository.deleteByReply_ReplyIdAndUser_Email(replyId, email);
    } else {
      replyLikeRepository.save(
          ReplyLike.builder()
              .reply(reply)
              .user(user)
              .build());
>>>>>>> origin/feature/jylee_2
    }

    return replyLikeRepository.countByReply_ReplyId(replyId);
  }

}

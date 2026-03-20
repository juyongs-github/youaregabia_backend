package com.music.music.board.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.board.entity.Reply;
import com.music.music.board.entity.ReplyLike;
import com.music.music.board.repository.ReplyLikeRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.user.entity.PointType;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import com.music.music.user.service.UserPointService;

import lombok.AllArgsConstructor;

@Service
@Transactional
@AllArgsConstructor
public class ReplyLikeService {
    private final ReplyRepository replyRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final UserRepository userRepository;
    private final UserPointService userPointService;

    public Map<String, Object> toggleLike(Long replyId, String email) {

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        // 좋아요 이미 눌렀는지 확인
        boolean alreadyLiked =
                replyLikeRepository.existsByReply_ReplyIdAndUser_Email(replyId, email);

        // 이미 눌렀으면 좋아요를 취소, 유저 포인트 가감
        if (alreadyLiked) {
        replyLikeRepository.deleteByReply_ReplyIdAndUser_Email(replyId, email);
        // 취소 시 — 누른 사람에게서 1pt 차감
        userPointService.deductPoint(email, 1);
        } else {
                replyLikeRepository.save(ReplyLike.builder().reply(reply).user(user).build());
                // 좋아요 누를 때 — 누른 사람에게 LIKE_GIVEN 지급
                userPointService.addLikeGivenPoint(email);
        }

        long likeCount = replyLikeRepository.countByReply_ReplyId(replyId);
        boolean likedByMe = !alreadyLiked;
        return Map.of("likeCount", likeCount, "likedByMe", likedByMe);

    }

}

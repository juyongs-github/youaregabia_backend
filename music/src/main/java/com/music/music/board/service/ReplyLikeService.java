package com.music.music.board.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.board.entity.Reply;
import com.music.music.board.entity.ReplyLike;
import com.music.music.board.repository.ReplyLikeRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@Transactional
@AllArgsConstructor
public class ReplyLikeService {
    private final ReplyRepository replyRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final UserRepository userRepository;

    public Map<String, Object> toggleLike(Long replyId, String email) {

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        boolean alreadyLiked = replyLikeRepository.existsByReply_ReplyIdAndUser_Email(replyId, email);

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

        long likeCount = replyLikeRepository.countByReply_ReplyId(replyId);
        boolean likedByMe = !alreadyLiked;
        return Map.of("likeCount", likeCount, "likedByMe", likedByMe);
    }

}

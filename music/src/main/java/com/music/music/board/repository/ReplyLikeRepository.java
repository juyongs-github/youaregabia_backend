package com.music.music.board.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.board.entity.ReplyLike;

public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long>{
    boolean existsByReply_ReplyIdAndUser_Email(Long replyId, String email);

    void deleteByReply_ReplyIdAndUser_Email(Long replyId, String email);

    long countByReply_ReplyId(Long replyId);

    void deleteByUser_Id(Long userId);
}

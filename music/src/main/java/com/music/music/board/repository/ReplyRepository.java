package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.board.entity.Reply;

public interface ReplyRepository extends JpaRepository<Reply, Long>{
     // 특정 게시글의 댓글 조회 (최신순)
    List<Reply> findByBoard_BoardIdOrderByCreatedAtDesc(Long boardId);

    // 특정 게시글의 댓글 개수
    long countByBoard_BoardId(Long boardId);

}

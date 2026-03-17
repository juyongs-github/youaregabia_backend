package com.music.music.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.board.entity.BoardLike;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long>{
    
    boolean existsByBoard_BoardIdAndUser_Email(Long boardId, String email);

    void deleteByBoard_BoardIdAndUser_Email(Long boardId, String email);

    long countByBoard_BoardId(Long boardId);

    void deleteByUser_Id(Long userId);
}

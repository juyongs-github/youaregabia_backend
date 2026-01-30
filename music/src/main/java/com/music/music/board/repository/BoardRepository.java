package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardType;

public interface BoardRepository extends JpaRepository<Board, Long>{
    // 제목 완전일치
    Page<Board> findByTitle(String title, Pageable pageable);
    // 제목 부분일치 검색
    Page<Board> findByTitleContaining(String keyword, Pageable pageable);
}

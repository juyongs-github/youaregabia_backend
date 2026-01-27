package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardType;

public interface BoardRepository extends JpaRepository<Board, Long>{
    Page<Board> findByTitle(String title, Pageable pageable);
}

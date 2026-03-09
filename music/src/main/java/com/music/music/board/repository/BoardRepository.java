package com.music.music.board.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardGenre;
import com.music.music.board.entity.BoardType;

public interface BoardRepository extends JpaRepository<Board, Long>{
    // 제목 완전일치
    Page<Board> findByDeletedFalseAndTitle(String title, Pageable pageable);

    // 제목 부분일치 검색
    Page<Board> findByDeletedFalseAndTitleContaining(String keyword, Pageable pageable);

    // 장르 검색
    Page<Board> findByDeletedFalseAndBoardGenre(BoardGenre boardGenre, Pageable pageable);

    // 장르 + 제목 검색
    Page<Board> findByDeletedFalseAndBoardGenreAndTitleContaining(
            BoardGenre genre, String keyword, Pageable pageable);

    // 전체 조회
    Page<Board> findByDeletedFalse(Pageable pageable);

    Optional<Board> findByBoardIdAndDeletedFalse(Long boardId);
}

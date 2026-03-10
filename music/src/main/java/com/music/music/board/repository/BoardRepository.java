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
<<<<<<< HEAD
    Page<Board> findByTitleContaining(String keyword, Pageable pageable);
    // 내가 쓴 게시글
    List<Board> findByUser_EmailOrderByCreatedAtDesc(String email);
=======
    Page<Board> findByDeletedFalseAndTitleContaining(String keyword, Pageable pageable);

    // 장르 검색
    Page<Board> findByDeletedFalseAndBoardGenre(BoardGenre boardGenre, Pageable pageable);

    // 장르 + 제목 검색
    Page<Board> findByDeletedFalseAndBoardGenreAndTitleContaining(
            BoardGenre genre, String keyword, Pageable pageable);

    // 전체 조회
    Page<Board> findByDeletedFalse(Pageable pageable);

    Optional<Board> findByBoardIdAndDeletedFalse(Long boardId);

    // 자유게시판 전체 조회
    Page<Board> findByDeletedFalseAndBoardType(BoardType boardType, Pageable pageable); 

    // 자유게시판 키워드 검색
    Page<Board> findByDeletedFalseAndBoardTypeAndTitleContaining(
    BoardType boardType, String keyword, Pageable pageable);
>>>>>>> origin/feature/board-after-gitignore
}

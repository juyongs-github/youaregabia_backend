package com.music.music.board.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardGenre;
import com.music.music.board.entity.BoardType;

public interface BoardRepository extends JpaRepository<Board, Long>{
    void deleteByUser_Id(Long userId);
    // 제목 완전일치
    Page<Board> findByDeletedFalseAndTitle(String title, Pageable pageable);

    // 내가 쓴 게시글
    List<Board> findByUser_EmailOrderByCreatedAtDesc(String email);


    Optional<Board> findByBoardIdAndDeletedFalse(Long boardId);

    @Query("""
    select b from Board b 
    where b.deleted = false 
    and (:type is null or b.boardType = :type) 
    and (:genre is null or b.boardGenre = :genre) 
    and (:keyword is null 
        or b.title like %:keyword%
        or b.content like %:keyword%)
    """)
    Page<Board> findByDynamicSearch(
    @Param("type") BoardType type, 
    @Param("genre") BoardGenre genre, 
    @Param("keyword") String keyword, 
    Pageable pageable);

    // boardSong을 이용하여 Song 과 연결된 게시글-> 평론을 위해서
    @Query("""
    select b from Board b
    join BoardSong bs on bs.board = b
    where bs.song.id = :songId
    and b.boardType = 'CRITIC'
    and b.deleted = false
    order by b.boardId desc
    """)
    Page<Board> findCriticBoardsBySongId(@Param("songId") Long songId, Pageable pageable);
}

package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // ✅ 이거 추가

import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardGenre;
import com.music.music.board.entity.Reply;

<<<<<<< HEAD
public interface ReplyRepository extends JpaRepository<Reply, Long>{
     // 특정 게시글의 댓글 조회 (최신순)
    Page<Reply> findByBoard_BoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);

    // 특정 게시글의 댓글 개수
    long countByBoard_BoardId(Long boardId);

   // 최신순 전용
    @Query("""
    select new com.music.music.board.dto.ReplyResponseDto(
        r.replyId,
        r.content,
        u.name,
        (select count(rl1) from ReplyLike rl1 where rl1.reply = r), 
        (select count(rl2) from ReplyLike rl2 where rl2.reply = r and rl2.user.email = :email),
        r.createdAt,
        r.deleted)
    from Reply r
    join r.user u
    where r.board.boardId = :boardId and r.parentReply is null
    order by r.createdAt desc
    """)
    Page<ReplyResponseDto> findRepliesLatest(Long boardId, String email, Pageable pageable);
    

// 좋아요순 정렬 전용
    @Query("""
    select new com.music.music.board.dto.ReplyResponseDto(
        r.replyId,
        r.content,
        u.name,
        (select count(rl1) from ReplyLike rl1 where rl1.reply = r),
        (select count(rl2) from ReplyLike rl2 where rl2.reply = r and rl2.user.email = :email),
        r.createdAt,
        r.deleted)
    from Reply r
    join r.user u
    where r.board.boardId = :boardId and r.parentReply is null
    order by (select count(rl3) from ReplyLike rl3 where rl3.reply = r) desc, r.createdAt desc
    """)
    Page<ReplyResponseDto> findRepliesWithLikeInfo(Long boardId, String email, Pageable pageable);

    // 대댓글 조회 (부모 댓글 ID로)
    @Query("""
        select new com.music.music.board.dto.ReplyResponseDto(
            r.replyId, r.content, u.name,
            (select count(rl1) from ReplyLike rl1 where rl1.reply = r),
            (select count(rl2) from ReplyLike rl2 where rl2.reply = r and rl2.user.email = :email),
            r.createdAt, r.deleted)
        from Reply r
        join r.user u
        where r.parentReply.replyId = :parentReplyId
        order by r.createdAt asc
        """)
    List<ReplyResponseDto> findChildren(Long parentReplyId, String email);
}
=======
public interface ReplyRepository extends JpaRepository<Reply, Long> {

  Page<Reply> findByBoard_BoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);

  long countByBoard_BoardId(Long boardId);

  @Query("""
      select new com.music.music.board.dto.ReplyResponseDto(
          r.replyId,
          r.content,
          u.name,
          count(rl),
          sum(case when rl.user.email = :email then 1 else 0 end),
          r.createdAt
      )
      from Reply r
      join r.user u
      left join ReplyLike rl on rl.reply.replyId = r.replyId
      where r.board.boardId = :boardId
      group by r.replyId, r.content, u.name, r.createdAt
      order by r.createdAt desc
      """)
  Page<ReplyResponseDto> findRepliesLatest(
      @Param("boardId") Long boardId,
      @Param("email") String email,
      Pageable pageable);

  @Query("""
      select new com.music.music.board.dto.ReplyResponseDto(
          r.replyId,
          r.content,
          u.name,
          count(rl),
          sum(case when rl.user.email = :email then 1 else 0 end),
          r.createdAt
      )
      from Reply r
      join r.user u
      left join ReplyLike rl on rl.reply.replyId = r.replyId
      where r.board.boardId = :boardId
      group by r.replyId, r.content, u.name, r.createdAt
      order by count(rl) desc, r.createdAt desc
      """)
  Page<ReplyResponseDto> findRepliesWithLikeInfo(
      @Param("boardId") Long boardId,
      @Param("email") String email,
      Pageable pageable);
}
>>>>>>> origin/feature/jylee_2

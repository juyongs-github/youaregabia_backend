package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // ✅ 이거 추가

import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Reply;

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

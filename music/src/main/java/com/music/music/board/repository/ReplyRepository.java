package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Reply;

public interface ReplyRepository extends JpaRepository<Reply, Long>{
    void deleteByUser_Id(Long userId);
     // 특정 게시글의 댓글 조회 (최신순)
    Page<Reply> findByBoard_BoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);

    // 내가 쓴 댓글
    List<Reply> findByUser_EmailOrderByCreatedAtDesc(String email);

    // 특정 게시글의 댓글 개수
    long countByBoard_BoardId(Long boardId);

   // 최신순 전용
    @Query("""
    select new com.music.music.board.dto.ReplyResponseDto(
        r.replyId,
        r.content,
        u.name,
        u.email,
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
        u.email,
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
            r.replyId, r.content, u.name,u.email,
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

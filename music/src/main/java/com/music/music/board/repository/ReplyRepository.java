package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Reply;

public interface ReplyRepository extends JpaRepository<Reply, Long>{
     // 특정 게시글의 댓글 조회 (최신순)
    Page<Reply> findByBoard_BoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);

    // 특정 게시글의 댓글 개수
    long countByBoard_BoardId(Long boardId);

    // 최신순 전용
    // null 일 떄를 위해서 coalsece/
    @Query("""
    select new com.music.music.board.dto.ReplyResponseDto(
    r.replyId,
    r.content,
    u.name,
    coalesce(count(rl), 0),
    coalesce(sum(case when rl.user.email = :email then 1 else 0 end), 0),
    r.createdAt
    )
    from Reply r
    join r.user u
    left join ReplyLike rl on rl.reply.replyId = r.replyId and rl.user.email = :email
    where r.board.boardId = :boardId
    group by r.replyId, r.content, u.name, r.createdAt
    order by r.createdAt desc
    """)
    Page<ReplyResponseDto> findRepliesLatest(Long boardId,String email,Pageable pageable);
    

// 좋아요기준 댓글 정렬+좋아요 기능 jpql
// ResponseDto에 좋아요 수를 확인, 좋아요를 누른 id가 사용자와 같다면 
// 0을 더하고 아니라면 1을 더한다 
    @Query("""
    select new com.music.music.board.dto.ReplyResponseDto(
        r.replyId,
        r.content,
        u.name,
        coalesce(count(rl), 0),
        coalesce(sum(case when rl.user.email = :email then 1 else 0 end), 0),
        r.createdAt
    )
    from Reply r
    join r.user u
    left join ReplyLike rl on rl.reply.replyId = r.replyId and rl.user.email = :email
    where r.board.boardId = :boardId
    group by r.replyId, r.content, u.name, r.createdAt
    order by count(rl) desc, r.createdAt desc
    """)
    Page<ReplyResponseDto> findRepliesWithLikeInfo(
            Long boardId,
            String email,
            Pageable pageable
    );

}

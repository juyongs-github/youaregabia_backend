package com.music.music.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Reply;

public interface ReplyRepository extends JpaRepository<Reply, Long>{
     // 특정 게시글의 댓글 조회 (최신순)
    List<Reply> findByBoard_BoardIdOrderByCreatedAtDesc(Long boardId);

    // 특정 게시글의 댓글 개수
    long countByBoard_BoardId(Long boardId);
    

// 좋아요기준 댓글 정렬+좋아요 기능 jpql
// ResponseDto에 좋아요 수를 확인, 좋아요를 누른 id가 사용자와 같다면 
// 0을 더하고 아니라면 1을 더한다 
    @Query("""
    select new com.music.music.board.dto.ReplyResponseDto(
        r.replyId,
        r.content,
        u.name,
        count(rl),
        sum(case when rl.user.id = :userId then 1 else 0 end),
        r.createdAt
    )
    from Reply r
    join r.user u
    left join ReplyLike rl on rl.reply = r
    where r.board.boardId = :boardId
    group by r, u
    order by count(rl) desc, r.createdAt desc
    """)
    List<ReplyResponseDto> findRepliesWithLikeInfo(
            Long boardId,
            Long userId
    );

}

package com.music.music.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.music.music.board.dto.ReplyCreateDto;
import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Board;
import com.music.music.board.entity.Reply;
import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyLikeRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.board.repository.UserRepository;
import com.music.music.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ReplyLikeRepository replyLikeRepository;

    
    
    public List<ReplyResponseDto> getReplies(Long boardId, Long userId) {
    return replyRepository.findRepliesWithLikeInfo(boardId, userId);
    }


    

    public Long createReply(Long boardId, Long userId, ReplyCreateDto dto) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        Reply reply = Reply.builder()
                .board(board)
                .user(user)
                .content(dto.getContent())
                .build();

        return replyRepository.save(reply).getReplyId();
    }

    public void deleteReply(Long replyId, Long userId) {

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (!reply.getUser().getId().equals(userId)) {
            throw new IllegalStateException("댓글 삭제 권한이 없습니다.");
        }

        replyRepository.delete(reply);

    }
    @Transactional
public void updateReply(Long replyId, Long userId, ReplyCreateDto dto) {

    Reply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

    if (!reply.getUser().getId().equals(userId)) {
        throw new IllegalStateException("댓글 수정 권한이 없습니다.");
    }

    reply.updateContent(dto.getContent());
}
}


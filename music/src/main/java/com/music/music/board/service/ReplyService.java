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
import com.music.music.notification.service.NotificationService;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReplyService {
<<<<<<< HEAD
  private final ReplyRepository replyRepository;
  private final BoardRepository boardRepository;
  private final UserRepository userRepository;
  private final ReplyLikeRepository replyLikeRepository;
  private final NotificationService notificationService;
=======
    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ReplyLikeRepository replyLikeRepository;
>>>>>>> origin/feature/board-after-gitignore

    


    

    public Long createReply(Long boardId, String email, ReplyCreateDto dto) {

<<<<<<< HEAD
    replyRepository.save(reply);

    // 게시글 작성자에게 알림 (본인 댓글 제외)
    User boardAuthor = board.getUser();
    if (!boardAuthor.getEmail().equals(email)) {
      String msg = user.getName() + "님이 회원님의 게시글에 댓글을 남겼습니다: \"" + board.getTitle() + "\"";
      notificationService.createNotification(boardAuthor, msg, boardId);
    }

    return reply.getReplyId();
  }
=======
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
>>>>>>> origin/feature/board-after-gitignore

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
        
        Reply parentReply = null;
        if (dto.getParentReplyId() != null) {
        parentReply = replyRepository.findById(dto.getParentReplyId())
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다."));
    }

        Reply reply = Reply.builder()
                .board(board)
                .user(user)
                .content(dto.getContent())
                .parentReply(parentReply)
                .build();

        return replyRepository.save(reply).getReplyId();
    }

    public void deleteReply(Long replyId, String email) {

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        if (!reply.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("댓글 삭제 권한이 없습니다.");
        }

        reply.delete();

    }
    @Transactional
    public void updateReply(Long replyId, String email, ReplyCreateDto dto) {

    Reply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

    if (!reply.getUser().getEmail().equals(email)) {
        throw new IllegalStateException("댓글 수정 권한이 없습니다.");
    }

    reply.updateContent(dto.getContent());
}
}
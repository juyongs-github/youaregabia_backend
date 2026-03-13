package com.music.music.board.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardLike;
import com.music.music.board.repository.BoardLikeRepository;
import com.music.music.board.repository.BoardRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardLikeService {
    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final UserRepository userRepository;

    public Map<String, Object> toggleLike(Long boardId, String email) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        boolean alreadyLiked =
                boardLikeRepository.existsByBoard_BoardIdAndUser_Email(boardId, email);

        if (alreadyLiked) {
            boardLikeRepository.deleteByBoard_BoardIdAndUser_Email(boardId, email);
            board.decreaseLikeCount(); 
        } else {
            boardLikeRepository.save(
                    BoardLike.builder()
                            .board(board)
                            .user(user)
                            .build());
             board.increaseLikeCount();  
        }

        return Map.of(
        "likeCount", board.getLikeCount(),  // count 쿼리 대신 엔티티 값 사용
        "likedByMe", !alreadyLiked
    );
    }
}

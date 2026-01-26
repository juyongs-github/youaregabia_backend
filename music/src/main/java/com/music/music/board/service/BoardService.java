package com.music.music.board.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.board.dto.BoardDto;
import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardType;
import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyLikeRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.board.repository.UserRepository;
import com.music.music.common.PageRequestDTO;
import com.music.music.user.User;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final ReplyService replyService;


    public List<BoardDto> getBoardList(PageRequestDTO dto) {
        Page<Board> result = null;
        Pageable pageable = PageRequest.of(dto.getPage(), dto.getSize(), Sort.by("id").descending());
        result = boardRepository.findAll(pageable);

    
    List<BoardDto> dtoList = result.stream()
            .map(BoardDto::new)  // 댓글 없는 생성자 사용
            .toList();
}

    public BoardDto getBoardDetail(Long boardId, Long userId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        List<ReplyResponseDto> replies =
                replyRepository.findRepliesWithLikeInfo(boardId, userId);

                

        return new BoardDto(board, replies);
    }



    @Transactional
    public Long createBoard(Long userId, BoardDto dto) {

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

    Board board = Board.builder()
            .user(user)
            .boardType(BoardType.PLAYLIST_SHARE)
            .title(dto.getTitle())
            .content(dto.getContent())
            .build();

    return boardRepository.save(board).getBoardId();
}

    @Transactional
    public void updateBoard(Long boardId, Long userId, BoardDto dto) {

    Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

    if (!board.getUser().getId().equals(userId)) {
        throw new IllegalStateException("게시글 수정 권한이 없습니다.");
    }

    board.update(dto.getTitle(), dto.getContent());
}

    @Transactional
    public void deleteBoard(Long boardId, Long userId) {

    Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

    if (!board.getUser().getId().equals(userId)) {
        throw new IllegalStateException("게시글 삭제 권한이 없습니다.");
    }

    boardRepository.delete(board);
}
}
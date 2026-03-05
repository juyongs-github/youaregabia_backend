package com.music.music.board.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.board.common.dto.PageRequestDTO;
import com.music.music.board.common.dto.PageResultDTO;
import com.music.music.board.dto.BoardDto;
import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardType;
import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyLikeRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.user.entitiy.User;
import com.music.music.user.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
    private final BoardRepository boardRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final ReplyService replyService;

    public PageResultDTO<BoardDto> getBoardList(PageRequestDTO dto, String keyword) {
        log.info(" 요청 - page: {}, size: {}", dto.getPage(), dto.getSize());

        Page<Board> result = null;
        // 리액트에서는 1,2,3 순서로 카운트하지만 백JPA에선 0,1,2 식으로 카운팅
        // 때문에 값이 넘어올 때 -1 해주어야한다.
        Pageable pageable = PageRequest.of(dto.getPage() - 1, dto.getSize(),
                Sort.by("boardId").descending());

        // keyword가 있다면 검색, 없으면 전체 조회
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = boardRepository.findByTitleContaining(keyword.trim(), pageable);
        } else {
            result = boardRepository.findAll(pageable);
        }

        List<BoardDto> dtoList = result.stream()
                .map(BoardDto::new) // 댓글 없는 생성자 사용
                .toList();

        return PageResultDTO.<BoardDto>withAll()
                .dtoList(dtoList)
                .totalCount(result.getTotalElements())
                .pageRequestDTO(dto)
                .build();
    }

    public BoardDto getBoardDetail(Long boardId, String email, PageRequestDTO dto) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // email이 비어있거나 null이면 익명 사용자 처리
        String userEmail = (email == null || email.trim().isEmpty()) ? "" : email;

        // 댓글 페이징
        Pageable pageable = PageRequest.of(
                dto.getPage() - 1, // 프론트는 1부터, JPA는 0부터
                dto.getSize());

        Page<ReplyResponseDto> replyPage;

        if ("likes".equals(dto.getSort())) {
            replyPage = replyRepository.findRepliesWithLikeInfo(boardId, userEmail, pageable);
        } else {
            replyPage = replyRepository.findRepliesLatest(boardId, userEmail, pageable);
        }

        PageResultDTO<ReplyResponseDto> replies = PageResultDTO.<ReplyResponseDto>withAll()
                .dtoList(replyPage.getContent())
                .totalCount(replyPage.getTotalElements())
                .pageRequestDTO(dto)
                .build();

        return new BoardDto(board, replies);
    }

    @Transactional
    public Long createBoard(String email, BoardDto dto) {

        User user = userRepository.findByEmail(email)
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
    public void updateBoard(Long boardId, String email, BoardDto dto) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!board.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("게시글 수정 권한이 없습니다.");
        }

        board.update(dto.getTitle(), dto.getContent());
    }

    @Transactional
    public void deleteBoard(Long boardId, String email) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!board.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("게시글 삭제 권한이 없습니다.");
        }
        boardRepository.delete(board);
    }
}
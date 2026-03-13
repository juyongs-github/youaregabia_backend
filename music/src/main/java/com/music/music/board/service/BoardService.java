package com.music.music.board.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.usertype.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.api.repository.SongRepository;
import com.music.music.board.common.dto.PageRequestDTO;
import com.music.music.board.common.dto.PageResultDTO;
import com.music.music.board.dto.BoardDto;
import com.music.music.board.dto.BoardSongDto;
import com.music.music.board.dto.ReplyResponseDto;
import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardGenre;
import com.music.music.board.entity.BoardSong;
import com.music.music.board.entity.BoardType;
import com.music.music.board.repository.BoardLikeRepository;
import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.BoardSongRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.playlist.entity.Song;
import com.music.music.user.entity.Role;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@AllArgsConstructor
@Transactional
public class BoardService {
    private final BoardRepository boardRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final BoardSongRepository boardSongRepository;
    private final BoardLikeRepository boardLikeRepository;


    public PageResultDTO<BoardDto> getBoardList(PageRequestDTO dto, String keyword, String genre, String boardType) {
        log.info(" 요청 - page: {}, size: {}", dto.getPage(), dto.getSize());

        // 리액트에서는 1,2,3 순서로 카운트하지만 백JPA에선 0,1,2 식으로 카운팅
        // 때문에 값이 넘어올 때 -1 해주어야한다.
        Pageable pageable = PageRequest.of(dto.getPage()-1, dto.getSize(),
        Sort.by("boardId").descending());

        // keyword가 있다면 검색, 없으면 전체 조회
        BoardType typeEnum = (boardType != null && !boardType.isEmpty()) ? BoardType.valueOf(boardType) : null;
        BoardGenre genreEnum = (genre != null && !genre.isEmpty()) ? BoardGenre.valueOf(genre) : null;
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Board> result = boardRepository.findByDynamicSearch(typeEnum, genreEnum, searchKeyword, pageable);

        List<BoardDto> dtoList = result.stream()
        .map(BoardDto::new)
        .toList();

        return PageResultDTO.<BoardDto>withAll()
                .dtoList(dtoList)
                .totalCount(result.getTotalElements())
                .pageRequestDTO(dto)
                .build();
    }
    
    @Transactional
    public BoardDto getBoardDetail(Long boardId, String email, PageRequestDTO dto) {

        Board board = boardRepository.findByBoardIdAndDeletedFalse(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        
        // email이 비어있거나 null이면 익명 사용자 처리
        String userEmail = (email == null || email.trim().isEmpty()) ? "" : email;


        // 댓글 페이징
        Pageable pageable = PageRequest.of(
            dto.getPage() - 1,  // 프론트는 1부터, JPA는 0부터
            dto.getSize());

        Page<ReplyResponseDto> replyPage;
        
    if ("likes".equals(dto.getSort())) {
        replyPage = replyRepository.findRepliesWithLikeInfo(boardId, userEmail, pageable);
    } else {
        replyPage = replyRepository.findRepliesLatest(boardId, userEmail, pageable);
    }

        // 기존 replyPage 가져온 후
        List<ReplyResponseDto> dtoList = replyPage.getContent().stream()
            .map(reply -> {
                List<ReplyResponseDto> children = 
                    replyRepository.findChildren(reply.getReplyId(), userEmail);
                reply.setChildren(children);
                return reply;
                })
        .toList();
        // BoardDto 상세용 생성자에 추가
        List<BoardSongDto> songDtos = boardSongRepository
        .findByBoard_BoardIdOrderByOrderIndex(board.getBoardId())
        .stream()
        .map(BoardSongDto::new)
        .toList();
        
        PageResultDTO<ReplyResponseDto> replies = PageResultDTO.<ReplyResponseDto>withAll()
                .dtoList(dtoList)
                .totalCount(replyPage.getTotalElements())
                .pageRequestDTO(dto)
                .build();
    

   BoardDto boardDto = new BoardDto(board, replies, songDtos);
    
    // 3. 사용자가 로그인 상태라면 좋아요 여부 확인
    if (email != null && !email.trim().isEmpty()) {
        boolean isLiked = boardLikeRepository.existsByBoard_BoardIdAndUser_Email(boardId, email);
        boardDto.setLikedByMe(isLiked);
    }
    
    board.increaseViewCount();
        
        return boardDto;
    }


    
    @Transactional
    public Long createBoard(String email, BoardDto dto) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

         // CRITIC 타입 게시글은 CRITIC 유저만 작성 가능
        if (BoardType.valueOf(dto.getBoardType()) == BoardType.CRITIC
        && user.getRole() != Role.CRITIC) {
        throw new IllegalStateException("평론 작성 권한이 없습니다.");
        }

        Board board = Board.builder()
            .user(user)
            .boardType(BoardType.valueOf(dto.getBoardType()))
            .boardGenre(BoardGenre.valueOf(dto.getBoardGenre()))
            .title(dto.getTitle())
            .content(dto.getContent())
            .build();
            Board saved = boardRepository.save(board);

        // songId가 있으면 BoardSong에 저장
        if (dto.getSongIds() != null && !dto.getSongIds().isEmpty()) {

            // 한 번에 조회 - N+1 방지
            List<Song> songs = songRepository.findAllById(dto.getSongIds());

            // songId 순서 보장을 위해 Map으로 변환
            Map<Long, Song> songMap = songs.stream()
                .collect(Collectors.toMap(Song::getId, s -> s));

            List<BoardSong> boardSongs = new ArrayList<>();

            for (int i = 0; i < dto.getSongIds().size(); i++) {
                Long sid = dto.getSongIds().get(i);
                Song song = songMap.get(sid);
                if (song == null) {
                    throw new IllegalArgumentException("곡이 존재하지 않습니다. ID: " + sid);
                }
                boardSongs.add(BoardSong.builder()
                    .board(saved)
                    .song(song)
                    .orderIndex(i)
                    .build());
            }

            boardSongRepository.saveAll(boardSongs);  // 한 번에 저장
        }

    return saved.getBoardId();
    }

    @Transactional
    public void updateBoard(Long boardId, String email, BoardDto dto) {

        Board board = boardRepository.findByBoardIdAndDeletedFalse(boardId)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!board.getUser().getEmail().equals(email)) {
        throw new IllegalStateException("게시글 수정 권한이 없습니다.");
        }

        board.update(dto.getTitle(), dto.getContent(),BoardGenre.valueOf(dto.getBoardGenre()));
    }

    @Transactional
    public void deleteBoard(Long boardId, String email) {

        Board board = boardRepository.findByBoardIdAndDeletedFalse(boardId)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!board.getUser().getEmail().equals(email)) {
        throw new IllegalStateException("게시글 삭제 권한이 없습니다.");
        }

        board.delete();
    }

    // 곡 기준으로 평론 조회
    @Transactional(readOnly = true)
    public PageResultDTO<BoardDto> getCriticBoards(Long songId, PageRequestDTO dto) {
        Pageable pageable = PageRequest.of(dto.getPage() - 1, dto.getSize(),
            Sort.by("boardId").descending());

        Page<Board> result = boardRepository.findCriticBoardsBySongId(songId, pageable);

        List<BoardDto> dtoList = result.stream()
            .map(BoardDto::new)
            .toList();

        return PageResultDTO.<BoardDto>withAll()
            .dtoList(dtoList)
            .totalCount(result.getTotalElements())
            .pageRequestDTO(dto)
            .build();
    }

    // 평론 조회
    @Transactional(readOnly = true)
    public PageResultDTO<BoardDto> getCriticList(PageRequestDTO dto, String keyword) {

    // 1. 페이지네이션 설정 (0-based index 처리)
    Pageable pageable = PageRequest.of(dto.getPage() - 1, dto.getSize(),
        Sort.by("boardId").descending());

    // 2. 검색어 전처리
    String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) 
                           ? keyword.trim() : null;

    // 3. 동적 검색 쿼리 호출 
    // Type은 CRITIC으로 고정, Genre는 null(전체)로 전달하여 필터링
    Page<Board> result = boardRepository.findByDynamicSearch(
        BoardType.CRITIC, 
        null, 
        searchKeyword, 
        pageable
    );

    // 4. 수동 생성자를 이용한 DTO 변환
    List<BoardDto> dtoList = result.stream()
        .map(BoardDto::new)
        .toList();

    // 5. 결과 빌드 및 반환
    return PageResultDTO.<BoardDto>withAll()
        .dtoList(dtoList)
        .totalCount(result.getTotalElements())
        .pageRequestDTO(dto)
        .build();
    }
}
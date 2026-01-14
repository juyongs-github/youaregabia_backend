package com.music.music.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.music.music.board.entity.Board;
import com.music.music.board.entity.BoardType;
import com.music.music.board.entity.Reply;
import com.music.music.board.repository.BoardRepository;
import com.music.music.board.repository.ReplyRepository;
import com.music.music.board.repository.UserRepository;
import com.music.music.user.Role;
import com.music.music.user.User;

@SpringBootTest
public class BoardReplyRepositoryTest {
        @Autowired
    BoardRepository boardRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Autowired
    UserRepository userRepository;

     @Test
    void board_reply_mapping_test() {
        // given
        User user = userRepository.save(
                User.builder()
                        .email("test@test.com")
                        .password("1234")
                        .nickname("기선")
                        .role(Role.USER)
                        .build()
        );

        Board board = boardRepository.save(
                Board.builder()
                        .user(user)
                        .boardType(BoardType.PLAYLIST_SHARE)
                        .title("테스트 게시글")
                        .content("게시글 내용")
                        .build()
        );

        Reply reply1 = replyRepository.save(
                Reply.builder()
                        .board(board)
                        .user(user)
                        .content("첫 번째 댓글")
                        .build()
        );

        Reply reply2 = replyRepository.save(
                Reply.builder()
                        .board(board)
                        .user(user)
                        .content("두 번째 댓글")
                        .build()
        );
}
}

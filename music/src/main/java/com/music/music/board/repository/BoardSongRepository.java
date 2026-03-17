package com.music.music.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.music.music.board.entity.BoardSong;

public interface BoardSongRepository extends JpaRepository <BoardSong, Long>{
    List<BoardSong> findByBoard_BoardIdOrderByOrderIndex(Long boardId);
}

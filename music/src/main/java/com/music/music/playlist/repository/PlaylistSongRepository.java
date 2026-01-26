package com.music.music.playlist.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.playlist.entity.PlaylistSong;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    // 중복체크
    boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

    // 특정 플레이리스트에 속한 노래들 조회
    @Query("select ps from PlaylistSong ps join fetch ps.song where ps.playlist.id = :playlistId")
    List<PlaylistSong> findByPlaylistIdWithSong(Long playlistId);
}

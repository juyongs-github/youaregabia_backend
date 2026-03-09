package com.music.music.playlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.playlist.entity.Playlist;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // 전체 조회
    List<Playlist> findAllByUserId(Long userId);

    // 상세 조회
    Optional<Playlist> findByIdAndUserId(Long playlistId, Long userId);

}

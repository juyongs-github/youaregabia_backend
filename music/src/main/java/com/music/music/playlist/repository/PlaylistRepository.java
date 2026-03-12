package com.music.music.playlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.constant.PlaylistType;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    boolean existsByUserIdAndTitle(Long userId, String title);

    List<Playlist> findByType(PlaylistType type);

    // 전체 조회
    List<Playlist> findAllByUserId(Long userId);

    // 상세 조회
    Optional<Playlist> findByIdAndUserId(Long playlistId, Long userId);

    void deleteByUserId(Long userId);
}

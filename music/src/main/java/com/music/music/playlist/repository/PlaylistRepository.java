package com.music.music.playlist.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.playlist.entity.Playlist;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    boolean existsByUserIdAndTitle(Long userId, String title);

}

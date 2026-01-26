package com.music.music.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.playlist.entity.Song;

public interface SongRepository extends JpaRepository<Song, Long> {
    Optional<Song> findById(Long id);
}

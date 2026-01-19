package com.music.music.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.entity.Song;

public interface SongRepository extends JpaRepository<Song, Long> {
    Optional<Song> findById(Long id);
}

package com.music.music.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.playlist.entity.Song;

public interface SongRepository extends JpaRepository<Song, Long> {
    Optional<Song> findById(Long id);

    @Query(value = "SELECT * FROM song ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Song findRandomSong();
}

package com.music.music.playlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.playlist.entity.Song;

public interface SongRepository extends JpaRepository<Song, Long> {

    @Query(value = "SELECT * FROM song ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Song findRandomSong();

    @Query(value = "SELECT * FROM song WHERE LOWER(track_name) LIKE LOWER(CONCAT('%', :trackName, '%')) AND LOWER(artist_name) LIKE LOWER(CONCAT('%', :artistName, '%')) LIMIT 1", nativeQuery = true)
    Optional<Song> findByTrackNameAndArtistNameLike(@Param("trackName") String trackName, @Param("artistName") String artistName);

    @Query(value = "SELECT * FROM song WHERE LOWER(track_name) LIKE LOWER(CONCAT('%', :trackName, '%')) LIMIT 1", nativeQuery = true)
    Optional<Song> findByTrackNameLike(@Param("trackName") String trackName);

    @Query(value = "SELECT * FROM song ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<Song> findRandomSongs(int limit);

}

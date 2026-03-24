package com.music.music.api.repository;

import com.music.music.api.entity.SpotifyTrackCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotifyTrackCacheRepository extends JpaRepository<SpotifyTrackCache, Long> {
    Optional<SpotifyTrackCache> findByTrackNameAndArtistName(String trackName, String artistName);
}

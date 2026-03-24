package com.music.music.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "spotify_track_cache",
       uniqueConstraints = @UniqueConstraint(columnNames = {"track_name", "artist_name"}))
@Data
@NoArgsConstructor
public class SpotifyTrackCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "track_name", nullable = false)
    private String trackName;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(name = "spotify_track_id")
    private String spotifyTrackId; // null = Spotify에서 못 찾은 곡 (재요청 방지)

    public SpotifyTrackCache(String trackName, String artistName, String spotifyTrackId) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.spotifyTrackId = spotifyTrackId;
    }
}

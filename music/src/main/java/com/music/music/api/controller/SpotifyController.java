package com.music.music.api.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.api.service.SpotifyApiService;

@RestController
public class SpotifyController {

    @Autowired
    SpotifyApiService spotifyApiService;

    @GetMapping("/api/spotify/track")
    public ResponseEntity<Map<String, String>> getSpotifyTrackId(
            @RequestParam String trackName,
            @RequestParam String artistName) {

        String trackId = spotifyApiService.searchTrackId(trackName, artistName);
        if (trackId == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("trackId", trackId));
    }
}

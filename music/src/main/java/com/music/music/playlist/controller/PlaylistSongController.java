package com.music.music.playlist.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.playlist.entity.PlaylistSong;
import com.music.music.playlist.service.PlaylistSongService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/song")
@RequiredArgsConstructor
@RestController
public class PlaylistSongController {

    private final PlaylistSongService playlistSongService;

    @PostMapping("/playlist/{playlistId}/song/{songId}")
    public ResponseEntity<Void> postSong(@PathVariable Long playlistId, @PathVariable Long songId) {
        playlistSongService.addSongToPlaylist(playlistId, songId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("playlist-song/{playlistSongId}")
    public ResponseEntity<Void> removeSong(@PathVariable Long playlistSongId) {
        playlistSongService.removeSongFromPlaylist(playlistSongId);

        return ResponseEntity.noContent().build();
    }
}

package com.music.music.playlist.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.api.entity.SongDTO;
import com.music.music.playlist.service.PlaylistSongService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@RestController
public class PlaylistSongController {

    private final PlaylistSongService playlistSongService;

    // 곡 추가
    @PostMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<Void> postSongToPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {

        playlistSongService.addSongToPlaylist(playlistId, songId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 곡 삭제
    @DeleteMapping("/songs/{playlistSongId}")
    public ResponseEntity<Void> removeSongFromPlaylist(@PathVariable Long playlistSongId) {
        playlistSongService.removeSongFromPlaylist(playlistSongId);

        return ResponseEntity.noContent().build();
    }

    // 곡 조회
    @GetMapping("/{playlistId}/songs")
    public ResponseEntity<List<SongDTO>> getSongsByPlaylist(@PathVariable Long playlistId) {
        return ResponseEntity.ok(playlistSongService.getSongsByPlaylist(playlistId));
    }

}
package com.music.music.playlist.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.api.entity.SongDTO;
import com.music.music.playlist.dto.CollaboSongDto;
import com.music.music.playlist.service.PlaylistSongService;

import lombok.RequiredArgsConstructor;

@RequestMapping("/playlist")
@RequiredArgsConstructor
@RestController
public class PlaylistSongController {

    private final PlaylistSongService playlistSongService;

    // 일반 플레이리스트 곡 조회
    @GetMapping("/{playlistId}/songs")
    public ResponseEntity<List<SongDTO>> getPlaylistSongs(@PathVariable Long playlistId) {
        return ResponseEntity.ok(playlistSongService.getPlaylistSongs(playlistId));
    }

    // 공동 플레이리스트 곡 조회 (투표수 + 등록자 포함)
    @GetMapping("/{playlistId}/collabo/songs")
    public ResponseEntity<List<CollaboSongDto>> getCollaboSongs(
            @PathVariable Long playlistId,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(playlistSongService.getCollaboSongs(playlistId, email));
    }

    // 작성자 직접 추가
    @PostMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<Void> addSongDirectly(
            @PathVariable Long playlistId,
            @PathVariable Long songId,
            @RequestParam String email) {
        try {
            playlistSongService.addSongDirectly(playlistId, songId, email);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).build();
        }
    }

    // 곡 삭제 (작성자 or 등록자)
    @DeleteMapping("/songs/{playlistSongId}")
    public ResponseEntity<Void> removeSong(
            @PathVariable Long playlistSongId,
            @RequestParam String email) {
        try {
            playlistSongService.removeSong(playlistSongId, email);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 투표 (최대 3개)
    @PostMapping("/{playlistId}/songs/{playlistSongId}/vote")
    public ResponseEntity<String> vote(
            @PathVariable Long playlistId,
            @PathVariable Long playlistSongId,
            @RequestParam String email) {
        try {
            playlistSongService.vote(playlistSongId, email);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    // reason 수정 (등록자만)
    @PatchMapping("/songs/{playlistSongId}/reason")
    public ResponseEntity<Void> updateReason(
            @PathVariable Long playlistSongId,
            @RequestParam String email,
            @RequestParam String reason) {
        try {
            playlistSongService.updateReason(playlistSongId, email, reason);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 투표 취소
    @DeleteMapping("/{playlistId}/songs/{playlistSongId}/vote")
    public ResponseEntity<Void> unvote(
            @PathVariable Long playlistId,
            @PathVariable Long playlistSongId,
            @RequestParam String email) {
        try {
            playlistSongService.unvote(playlistSongId, email);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

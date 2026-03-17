package com.music.music.playlist.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.playlist.dto.CollaboPlaylistResponseDto;
import com.music.music.playlist.dto.PlaylistDTO;
import com.music.music.playlist.service.PlaylistService;
import com.music.music.playlist.service.PlaylistSongService;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PutMapping;

@RequestMapping("/playlist")
@RequiredArgsConstructor
@RestController
public class PlaylistController {
    private final PlaylistService playlistService;
    private final PlaylistSongService playlistSongService;
    private final UserRepository userRepository;

    // 유저 정보 불러오기
    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("유저 없음"));
    }

    // 플레이리스트 전체 조회 (/playlist/all + GET)
    @GetMapping("/all")
    public List<PlaylistDTO> getAllPlaylists() {
        User user = getCurrentUser();
        return playlistService.getAllPlaylists(user.getId());
    }

    // playlist_song 테이블 매핑 될 수 있도록 수정
    @PostMapping
    public ResponseEntity<PlaylistDTO> createPlaylist(
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "title") String title,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "songIds", required = false) List<Long> songIds,
            @RequestParam(name = "type") String type,
            @RequestParam(name = "genre", required = false) String genre) {
        User user = getCurrentUser();
        try {
            PlaylistDTO playlistDTO = playlistService.createPlaylist(file, title, description, songIds, user, type,
                    genre);
            return ResponseEntity.ok(playlistDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).build();
        }
    }

    // 플레이리스트 상세 조회 (/playlist/{id} + GET)
    @GetMapping("/{id}")
    public PlaylistDTO getPlaylist(@PathVariable("id") Long id) {
        User user = getCurrentUser();
        return playlistService.getPlaylist(id, user.getId());
    }

    // 플레이리스트 수정
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public PlaylistDTO updatePlaylist(
            @PathVariable Long id,
            @RequestPart PlaylistDTO dto,
            @RequestPart(required = false) MultipartFile file) {
        return playlistService.updatePlaylist(id, dto, file);
    }

    // 플레이리스트 삭제
    @DeleteMapping("/{id}")
    public String deletePlaylist(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return "삭제완료";
    }

    // ─── 공동 플레이리스트 ────────────────────────────────────

    // 공동 플레이리스트 목록
    @GetMapping("/collabo/all")
    public List<CollaboPlaylistResponseDto> getAllCollaborativePlaylists(
            @RequestParam(required = false) String email) {
        return playlistService.getAllCollaborativePlaylists(email);
    }

    // 공동 플레이리스트 단건 조회
    @GetMapping("/collabo/{id}")
    public ResponseEntity<CollaboPlaylistResponseDto> getCollabPlaylist(
            @PathVariable Long id,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(playlistService.getCollabPlaylist(id, email));
    }

    // 공동 플레이리스트 참여 재개 (마감 → 진행중)
    @PutMapping("/collabo/{id}/reopen")
    public ResponseEntity<Void> reopenPlaylist(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam LocalDateTime newDeadline) {
        try {
            playlistService.reopenPlaylist(id, email, newDeadline);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 공동 플레이리스트 좋아요
    @PostMapping("/collabo/{id}/like")
    public ResponseEntity<Void> likePlaylist(@PathVariable Long id, @RequestParam String email) {
        try {
            playlistService.likePlaylist(id, email);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).build();
        }
    }

    // 공동 플레이리스트 좋아요 취소
    @DeleteMapping("/collabo/{id}/like")
    public ResponseEntity<Void> unlikePlaylist(@PathVariable Long id, @RequestParam String email) {
        try {
            playlistService.unlikePlaylist(id, email);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 공동 플레이리스트 → 내 플레이리스트로 가져오기 (투표순 상위 10곡)
    @PostMapping("/collabo/{id}/import")
    public ResponseEntity<Long> importCollabo(
            @PathVariable Long id,
            @RequestParam String email) {
        try {
            Long newPlaylistId = playlistService.importCollabo(id, email);
            return ResponseEntity.ok(newPlaylistId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 곡 추가 (참여자, 최대 5곡)
    @PostMapping("/{playlistId}/songs/suggest")
    public ResponseEntity<String> suggestSong(
            @PathVariable Long playlistId,
            @RequestParam Long songId,
            @RequestParam String email,
            @RequestParam(required = false) String reason) {
        try {
            playlistSongService.suggestSong(playlistId, songId, email, reason);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}

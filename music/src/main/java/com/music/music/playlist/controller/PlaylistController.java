package com.music.music.playlist.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.playlist.dto.PlaylistDTO;
import com.music.music.playlist.dto.CollaboPlaylistParticipantDTO;
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
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/playlist")
@RequiredArgsConstructor
@RestController
public class PlaylistController {
  private final PlaylistService playlistService;
  private final PlaylistSongService playlistSongService;
  private final UserRepository userRepository;

  // 플레이리스트 전체 조회 (/playlist/all + GET)
  @GetMapping("/all")
  public List<PlaylistDTO> getAllPlaylists(@RequestParam("email") String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("유저 없음"));
    return playlistService.getAllPlaylists(user.getId());
  }

  // playlist_song 테이블 매핑 될 수 있도록 수정
  @PostMapping
  public ResponseEntity<PlaylistDTO> createPlaylist(
      @RequestParam(name = "email") String email,
      @RequestPart(name = "file", required = false) MultipartFile file,
      @RequestParam(name = "title") String title,
      @RequestParam(name = "description") String description,
      @RequestParam(name = "songIds", required = false) List<Long> songIds,
      @RequestParam(name = "type") String type,
      @RequestParam(name = "genre", required = false) String genre) {
    User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("해당 유저 없음"));
    try {
      PlaylistDTO playlistDTO = playlistService.createPlaylist(file, title, description, songIds, user, type, genre);
      return ResponseEntity.ok(playlistDTO);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(409).build();
    }
  }

  // 플레이리스트 상세 조회 (/playlist/{id} + GET)
  @GetMapping("/{id}")
  public PlaylistDTO getPlaylist(@PathVariable("id") Long id, @RequestParam("email") String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("유저 없음"));
    return playlistService.getPlaylist(id, user.getId());
  }

  // 플레이리스트 수정 (/playlist/{id} + PUT)
  @PutMapping(value = "/{id}", consumes = "multipart/form-data")
  public PlaylistDTO updatePlaylist(
      @PathVariable("id") Long id,
      @RequestPart("dto") PlaylistDTO dto,
      @RequestPart(value = "file", required = false) MultipartFile file) {

    return playlistService.updatePlaylist(id, dto, file);
  }

  // 플레이리스트 삭제 (/playlist/{id} + DELETE)
  @DeleteMapping("/{id}")
  public String deletePlaylist(@PathVariable Long id) {
    playlistService.deletePlaylist(id);

    return "삭제완료";
  }

  /*
      공동 플레이리스트 관련 API
  */

  // 공동 플레이리스트 전체 조회 (/playlist/collabo/all + GET)
  @GetMapping("/collabo/all")
  public List<PlaylistDTO> getAllCollaborativePlaylists() {
    return playlistService.getAllCollaborativePlaylists();
  }

  // 곡 제안 (/playlist/{playlistId}/songs/suggest + POST)
  @PostMapping("/{playlistId}/songs/suggest")
  public ResponseEntity<Void> suggestSong(
      @PathVariable Long playlistId,
      @RequestParam("songId") Long songId,
      @RequestParam("email") String email) {
    try {
      playlistSongService.suggestSong(playlistId, songId, email);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(409).build();
    }
  }

  // 대기 중인 곡 제안 목록 (/playlist/{playlistId}/songs/pending + GET)
  @GetMapping("/{playlistId}/songs/pending")
  public ResponseEntity<List<CollaboPlaylistParticipantDTO>> getPendingSuggestions(
      @PathVariable Long playlistId,
      @RequestParam("email") String email) {
    try {
      return ResponseEntity.ok(playlistSongService.getPendingSuggestions(playlistId, email));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(403).build();
    }
  }

  // 곡 제안 수락 (/playlist/{playlistId}/songs/pending/{suggestionId}/accept + PUT)
  @PutMapping("/{playlistId}/songs/pending/{suggestionId}/accept")
  public ResponseEntity<Void> acceptSuggestion(
      @PathVariable Long playlistId,
      @PathVariable Long suggestionId,
      @RequestParam("email") String email) {
    try {
      playlistSongService.acceptSuggestion(playlistId, suggestionId, email);
      return ResponseEntity.ok().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(403).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(409).build();
    }
  }

  // 곡 제안 거절 (/playlist/{playlistId}/songs/pending/{suggestionId} + DELETE)
  @DeleteMapping("/{playlistId}/songs/pending/{suggestionId}")
  public ResponseEntity<Void> rejectSuggestion(
      @PathVariable Long playlistId,
      @PathVariable Long suggestionId,
      @RequestParam("email") String email) {
    try {
      playlistSongService.rejectSuggestion(playlistId, suggestionId, email);
      return ResponseEntity.ok().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(403).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

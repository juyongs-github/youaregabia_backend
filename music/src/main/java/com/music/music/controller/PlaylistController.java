package com.music.music.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.dto.PlaylistDTO;
import com.music.music.service.PlaylistService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/playlists")
@RequiredArgsConstructor
@RestController
public class PlaylistController {
    private final PlaylistService playlistService;

    // CREATE
    @PostMapping("/add")
    public ResponseEntity<PlaylistDTO> postPlaylist(@RequestBody PlaylistDTO dto) {
        PlaylistDTO PlaylistDTO = playlistService.createPlaylist(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(PlaylistDTO);
    }

    // READ

    // 플레이리스트 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistDTO> getPlaylist(@RequestParam Long id) {
        return ResponseEntity.ok(playlistService.getPlaylist(id));
    }

    // 플레이리스트 전체 목록 조회
    @GetMapping("/")
    public ResponseEntity<List<PlaylistDTO>> getAllPlaylists() {
        return ResponseEntity.ok(playlistService.getAllPlaylists());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<PlaylistDTO> putPlaylist(@PathVariable Long id, @RequestBody PlaylistDTO dto) {

        return ResponseEntity.ok(playlistService.updatePlaylist(id, dto));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable Long id) {
        playlistService.deletePlaylist(id);

        return ResponseEntity.noContent().build();

    }

}

package com.music.music.playlist.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.playlist.dto.PlaylistDTO;
import com.music.music.playlist.service.PlaylistService;
import com.music.music.user.entitiy.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
<<<<<<< HEAD
=======
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.playlist.dto.PlaylistDTO;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.service.PlaylistService;
import com.music.music.user.entitiy.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
>>>>>>> origin/feature/jylee_2
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@RestController
public class PlaylistController {
    private final PlaylistService playlistService;
    private final UserRepository userRepository;
<<<<<<< HEAD

    // CREATE
    // @PostMapping("/add")
    // public ResponseEntity<PlaylistDTO> postPlaylist(@RequestBody PlaylistDTO dto)
    // {
    // PlaylistDTO PlaylistDTO = playlistService.createPlaylist(dto);

    // return ResponseEntity.status(HttpStatus.CREATED).body(PlaylistDTO);
    // }

    // READ

    // 플레이리스트 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistDTO> getPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getPlaylist(id));
    }

    // 플레이리스트 전체 목록 조회
    // @GetMapping("/")
    // public ResponseEntity<List<PlaylistDTO>> getAllPlaylists() {
    // return ResponseEntity.ok(playlistService.getAllPlaylists());
    // }

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

    @GetMapping("/all")
    public List<PlaylistDTO> getAllPlaylists() {
        return playlistService.getAllPlaylists();
    }

    @PostMapping
    public ResponseEntity<Void> createPlaylist(
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam String description) {
        User user = userRepository.findById(1L).orElseThrow(() -> new IllegalStateException("해당 유저 없음"));
        playlistService.createPlaylist(file, title, description, user);
        return ResponseEntity.ok().build();
    }
=======
>>>>>>> origin/feature/jylee_2

    @GetMapping("/all")
    public List<PlaylistDTO> getAllPlaylists() {
        return playlistService.getAllPlaylists();
    }
    

    @PostMapping
    public ResponseEntity<Void> createPlaylist(
        @RequestParam MultipartFile file, 
        @RequestParam String title, 
        @RequestParam String description) {
        User user = userRepository.findById(1L).orElseThrow(() -> new IllegalStateException("해당 유저 없음"));
        playlistService.createPlaylist(file, title, description, user);
        return ResponseEntity.ok().build();
    }
    
}

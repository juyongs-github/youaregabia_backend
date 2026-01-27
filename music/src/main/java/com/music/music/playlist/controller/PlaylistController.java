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

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/playlist")
@RequiredArgsConstructor
@RestController
public class PlaylistController {
    private final PlaylistService playlistService;
    private final UserRepository userRepository;

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

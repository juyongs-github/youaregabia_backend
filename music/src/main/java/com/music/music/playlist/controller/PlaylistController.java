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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequestMapping("/playlist")
@RequiredArgsConstructor
@RestController
public class PlaylistController {
    private final PlaylistService playlistService;
    private final UserRepository userRepository;

    // 플레이리스트 전체 조회 (/playlist/all + GET)
    @GetMapping("/all")
    public List<PlaylistDTO> getAllPlaylists() {
        return playlistService.getAllPlaylists();
    }

    // 플레이리스트 생성 (/playlist + POST)
    @PostMapping
    public ResponseEntity<Void> createPlaylist(
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam String description) {
        User user = userRepository.findById(1L).orElseThrow(() -> new IllegalStateException("해당 유저 없음"));
        playlistService.createPlaylist(file, title, description, user);
        return ResponseEntity.ok().build();
    }

    // 수정한 부분

    // 플레이리스트 상세 조회 (/playlist/{id} + GET)
    @GetMapping("/{id}")
    public PlaylistDTO getPlaylist(@PathVariable Long id) {

        return playlistService.getPlaylist(id);
    }

    // 플레이리스트 수정 (/playlist/{id} + PUT)
    @PutMapping("/{id}")
    public PlaylistDTO putPlaylist(@PathVariable Long id, @RequestBody PlaylistDTO dto) {

        return playlistService.updatePlaylist(id, dto);
    }

    // 플레이리스트 삭제 (/playlist/{id} + DELETE)
    @DeleteMapping("/{id}")
    public String deletePlaylist(@PathVariable Long id) {
        playlistService.deletePlaylist(id);

        return "삭제완료";
    }
}

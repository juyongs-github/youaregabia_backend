package com.music.music.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.dto.PlaylistDTO;
import com.music.music.entity.Playlist;
import com.music.music.repository.PlaylistRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Transactional
@Log4j2
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    // CREATE
    // 플레이리스트 생성
    public Playlist createPlaylist(PlaylistDTO dto) {
        // 이미지 기본값
        String imageUrl = dto.getImageUrl();
        if (imageUrl == null) {
            imageUrl = "/images/default-playlist.png";
        }

        // dto => entity
        Playlist playlist = Playlist.builder()
                .imageUrl(imageUrl)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .build();

        return playlistRepository.save(playlist);
    }

    // READ
    // 플레이리스트 상세 조회
    @Transactional(readOnly = true)
    public Playlist getPlaylist(Long id) {
        return playlistRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));
    }

    // 플레이리스트 목록 조회
    @Transactional(readOnly = true)
    public List<Playlist> getAllPlaylists() {
        return playlistRepository.findAll();
    }

    // UPDATE

    // DELETE
    public void deletePlaylist(Long id) {
        if (!playlistRepository.existsById(id)) {
            throw new IllegalArgumentException("플레이리스트를 찾을 수 없습니다.");
        }
        playlistRepository.deleteById(id);
    }
}

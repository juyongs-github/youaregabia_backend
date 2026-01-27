package com.music.music.playlist.service;

// import static org.mockito.Mockito.description;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.common.service.FileService;
import com.music.music.playlist.dto.PlaylistDTO;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.constant.PlaylistType;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.user.entitiy.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final FileService fileService;

    /*
     * =========================
     * Entity → DTO
     * =========================
     */
    private PlaylistDTO toDto(Playlist playlist) {
        return PlaylistDTO.builder()
                .id(playlist.getId())
                .title(playlist.getTitle())
                .description(playlist.getDescription())
                .imageUrl(playlist.getImageUrl())
                .type(playlist.getType())
                .songCount(
                        playlist.getPlaylistSongs() != null
                                ? playlist.getPlaylistSongs().size()
                                : 0)
                .build();
    }

    /*
     * =========================
     * CREATE
     * =========================
     */
<<<<<<< HEAD
    // public PlaylistDTO createPlaylist(PlaylistDTO dto) {

    // String imageUrl = dto.getImageUrl() != null
    // ? dto.getImageUrl()
    // : "/images/default-playlist.png";

    // Playlist playlist = Playlist.builder()
    // .title(dto.getTitle())
    // .description(dto.getDescription())
    // .imageUrl(imageUrl)
    // .type(PlaylistType.MYPLAYLIST)
    // .build();

    // playlistRepository.save(playlist);

    // return toDto(playlist);
    // }

    public PlaylistDTO createPlaylist(MultipartFile file, String title, String description, User user) {

        // String imageUrl = dto.getImageUrl() != null
        // ? dto.getImageUrl()
        // : "/images/default-playlist.png";

        // Playlist playlist = Playlist.builder()
        // .title(dto.getTitle())
        // .description(dto.getDescription())
        // .imageUrl(imageUrl)
        // .type(PlaylistType.MYPLAYLIST)
        // .build();
=======
    public PlaylistDTO createPlaylist(MultipartFile file, String title, String description, User user) {

        // String imageUrl = dto.getImageUrl() != null
        //         ? dto.getImageUrl()
        //         : "/images/default-playlist.png";

        // Playlist playlist = Playlist.builder()
        //         .title(dto.getTitle())
        //         .description(dto.getDescription())
        //         .imageUrl(imageUrl)
        //         .build();
>>>>>>> origin/feature/jylee_2

        String imageUrl;

        if (file != null && !file.isEmpty()) {
            imageUrl = fileService.upload(file);
        } else {
            imageUrl = "/images/default-playlist.png";
        }

        Playlist playlist = Playlist.builder()
<<<<<<< HEAD
                .user(user)
                .type(PlaylistType.MYPLAYLIST)
                .title(title)
                .description(description)
                .imageUrl(imageUrl)
                .build();
=======
            .user(user)
            .type(PlaylistType.MYPLAYLIST)
            .title(title)
            .description(description)
            .imageUrl(imageUrl)
            .build();
>>>>>>> origin/feature/jylee_2

        playlistRepository.save(playlist);

        return toDto(playlist);
    }

    /*
     * =========================
     * READ
     * =========================
     */

    // 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    public PlaylistDTO getPlaylist(Long id) {

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        return toDto(playlist);
    }

    // 플레이리스트 전체 목록 조회
    @Transactional(readOnly = true)
    public List<PlaylistDTO> getAllPlaylists() {
        return playlistRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /*
     * =========================
     * UPDATE
     * =========================
     */
    public PlaylistDTO updatePlaylist(Long id, PlaylistDTO dto) {

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (dto.getTitle() != null) {
            playlist.changeTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            playlist.changeDescription(dto.getDescription());
        }
        if (dto.getImageUrl() != null) {
            playlist.changeImageUrl(dto.getImageUrl());
        }

        return toDto(playlist);
    }

    /*
     * =========================
     * DELETE
     * =========================
     */
    public void deletePlaylist(Long id) {

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        playlistRepository.delete(playlist);
    }
}
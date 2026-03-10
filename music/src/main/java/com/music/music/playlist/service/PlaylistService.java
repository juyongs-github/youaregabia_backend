package com.music.music.playlist.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.api.repository.SongRepository;
import com.music.music.board.common.service.FileService;
import com.music.music.playlist.dto.PlaylistDTO;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.entity.constant.PlaylistType;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final SongRepository songRepository;
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
    @Transactional
    public PlaylistDTO createPlaylist(MultipartFile file, String title, String description, List<Long> songIds,
            User user, String type, String genre) {

        if (playlistRepository.existsByUserIdAndTitle(user.getId(), title)) {
            throw new IllegalArgumentException("같은 제목의 플레이리스트가 이미 존재합니다.");
        }

        String imageUrl;

        if (file != null && !file.isEmpty()) {
            imageUrl = fileService.upload(file);
        } else {
            imageUrl = "/images/default-playlist.png";
        }

        Playlist playlist = Playlist.builder()
                .user(user)
                .type(PlaylistType.valueOf(type.toUpperCase()))
                .title(title)
                .description(description)
                .imageUrl(imageUrl)
                .genre(genre)
                .build();

        if (songIds != null) {
            List<Song> songs = songRepository.findAllById(songIds);
            for (Song song : songs) {
                playlist.addSong(song);
            }
        }

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
    public PlaylistDTO getPlaylist(Long playlistId, Long userId) {

        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        return toDto(playlist);
    }

    // 플레이리스트 전체 목록 조회
    @Transactional(readOnly = true)
    public List<PlaylistDTO> getAllPlaylists(Long userId) {

        return playlistRepository.findAllByUserId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 공동 플레이리스트 전체 조회
    @Transactional(readOnly = true)
    public List<PlaylistDTO> getAllCollaborativePlaylists() {
        return playlistRepository.findByType(PlaylistType.COLLABORATIVE)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /*
     * =========================
     * UPDATE
     * =========================
     */
    public PlaylistDTO updatePlaylist(Long id, PlaylistDTO dto, MultipartFile file) {

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (dto.getTitle() != null) {
            playlist.changeTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            playlist.changeDescription(dto.getDescription());
        }
        if (file != null && !file.isEmpty()) {
            String imageUrl = fileService.upload(file);
            playlist.changeImageUrl(imageUrl);
        }

        playlistRepository.save(playlist);

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

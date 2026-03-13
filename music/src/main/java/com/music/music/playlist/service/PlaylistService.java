package com.music.music.playlist.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.api.repository.SongRepository;
import com.music.music.board.common.service.FileService;
import com.music.music.playlist.dto.CollaboPlaylistResponseDto;
import com.music.music.playlist.dto.PlaylistDTO;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.PlaylistLike;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.entity.constant.PlaylistType;
import com.music.music.playlist.repository.PlaylistLikeRepository;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.playlist.repository.PlaylistSongRepository;
import com.music.music.playlist.repository.PlaylistSongVoteRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistSongVoteRepository playlistSongVoteRepository;
    private final PlaylistLikeRepository playlistLikeRepository;
    private final SongRepository songRepository;
    private final FileService fileService;
    private final UserRepository userRepository;

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
            User user, String type, String genre, LocalDateTime deadline) {

        if (playlistRepository.existsByUserIdAndTitle(user.getId(), title)) {
            throw new IllegalArgumentException("같은 제목의 플레이리스트가 이미 존재합니다.");
        }

        if ("COLLABORATIVE".equalsIgnoreCase(type) && deadline == null) {
            throw new IllegalArgumentException("공동 플레이리스트는 마감일이 필수입니다.");
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
                .deadline(deadline)
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

    // 공동 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    public CollaboPlaylistResponseDto getCollabPlaylist(Long playlistId, String email) {
        Playlist p = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));
        boolean hasLiked = (email != null && !email.isBlank())
                && userRepository.findByEmail(email)
                        .map(u -> playlistLikeRepository.existsByPlaylistIdAndUserId(playlistId, u.getId()))
                        .orElse(false);
        return CollaboPlaylistResponseDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .genre(p.getGenre())
                .creatorEmail(p.getUser().getEmail())
                .creatorName(p.getUser().getName())
                .songCount(playlistSongRepository.countByPlaylistId(p.getId()))
                .participantCount(playlistSongRepository.countDistinctSuggestorsByPlaylistId(p.getId()))
                .likeCount(playlistLikeRepository.countByPlaylistId(p.getId()))
                .hasLiked(hasLiked)
                .deadline(p.getDeadline())
                .deadlinePassed(p.isDeadlinePassed())
                .createdAt(p.getCreatedAt())
                .build();
    }

    // 공동 플레이리스트 전체 조회
    @Transactional(readOnly = true)
    public List<CollaboPlaylistResponseDto> getAllCollaborativePlaylists(String email) {
        List<Playlist> playlists = playlistRepository.findByTypeWithUser(PlaylistType.COLLABORATIVE);

        List<Long> playlistIds = playlists.stream().map(Playlist::getId).toList();

        Set<Long> likedIds = (email != null && !email.isBlank())
                ? Set.copyOf(playlistLikeRepository.findLikedPlaylistIdsByEmailAndPlaylistIds(playlistIds, email))
                : Set.of();

        return playlists.stream()
                .map(p -> CollaboPlaylistResponseDto.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .description(p.getDescription())
                        .imageUrl(p.getImageUrl())
                        .genre(p.getGenre())
                        .creatorEmail(p.getUser().getEmail())
                        .creatorName(p.getUser().getName())
                        .songCount(playlistSongRepository.countByPlaylistId(p.getId()))
                        .participantCount(playlistSongRepository.countDistinctSuggestorsByPlaylistId(p.getId()))
                        .likeCount(playlistLikeRepository.countByPlaylistId(p.getId()))
                        .hasLiked(likedIds.contains(p.getId()))
                        .deadline(p.getDeadline())
                        .deadlinePassed(p.isDeadlinePassed())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    // 공동 플레이리스트 좋아요
    public void likePlaylist(Long playlistId, String email) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (playlistLikeRepository.existsByPlaylistIdAndUserId(playlistId, user.getId())) {
            throw new IllegalArgumentException("이미 좋아요한 플레이리스트입니다.");
        }

        try {
            playlistLikeRepository.save(PlaylistLike.builder().playlist(playlist).user(user).build());
            playlistLikeRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 좋아요한 플레이리스트입니다.");
        }
    }

    // 공동 플레이리스트 참여 재개 (마감 → 진행중)
    public void reopenPlaylist(Long playlistId, String email, LocalDateTime newDeadline) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (!playlist.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("작성자만 참여를 재개할 수 있습니다.");
        }

        if (newDeadline == null || newDeadline.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("새 마감일은 현재 시간 이후여야 합니다.");
        }

        playlist.changeDeadline(newDeadline);
    }

    // 공동 플레이리스트 좋아요 취소
    public void unlikePlaylist(Long playlistId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        PlaylistLike like = playlistLikeRepository.findByPlaylistIdAndUserId(playlistId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("좋아요 내역이 없습니다."));

        playlistLikeRepository.delete(like);
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
        if (dto.getDeadline() != null) {
            playlist.changeDeadline(dto.getDeadline());
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
        if (!playlistRepository.existsById(id)) {
            throw new IllegalArgumentException("플레이리스트를 찾을 수 없습니다.");
        }

        // FK 의존 순서대로 삭제
        playlistSongVoteRepository.deleteByPlaylistId(id); // playlist_song_vote
        playlistLikeRepository.deleteByPlaylistId(id);     // playlist_like
        playlistRepository.deleteById(id);                 // playlist (cascade로 playlist_song, review 삭제)
    }
}

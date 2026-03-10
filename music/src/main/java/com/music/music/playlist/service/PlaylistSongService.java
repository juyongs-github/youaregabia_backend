package com.music.music.playlist.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.api.entity.SongDTO;
import com.music.music.api.repository.SongRepository;
import com.music.music.playlist.dto.CollaboPlaylistParticipantDTO;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.PlaylistSong;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.entity.CollaboPlaylistParticipant;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.playlist.repository.PlaylistSongRepository;
import com.music.music.playlist.repository.CollaboPlaylistParticipantRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaylistSongService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final CollaboPlaylistParticipantRepository songSuggestionRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    // 곡 조회
    public List<SongDTO> getSongsByPlaylist(Long playlistId) {
        return playlistSongRepository.findByPlaylistIdWithSong(playlistId)
                .stream()
                .map(this::toSongDto)
                .toList();
    }

    // Song → DTO
    private SongDTO toSongDto(PlaylistSong ps) {

        Song song = ps.getSong();


        return SongDTO.builder()
                .playlistSongId(ps.getId()) // *
                .id(song.getId())
                .trackName(song.getTrackName())
                .artistName(song.getArtistName())
                .previewUrl(song.getPreviewUrl())
                .imgUrl(song.getImgUrl())
                .releaseDate(song.getReleaseDate())
                .durationMs(song.getDurationMs())
                .genreName(song.getGenreName())
                .build();
    }

    private CollaboPlaylistParticipantDTO toSuggestionDto(CollaboPlaylistParticipant suggestion) {
        return CollaboPlaylistParticipantDTO.builder()
                .id(suggestion.getId())
                .songId(suggestion.getSong().getId())
                .trackName(suggestion.getSong().getTrackName())
                .artistName(suggestion.getSong().getArtistName())
                .imgUrl(suggestion.getSong().getImgUrl())
                .suggestedByEmail(suggestion.getSuggestedBy().getEmail())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }

    /*
     * =========================
     * 수록곡 목록 조회
     * GET /playlist/{playlistId}/songs
     * =========================
     */
    @Transactional(readOnly = true)
    public List<SongDTO> getPlaylistSongs(Long playlistId) {
        return playlistSongRepository.findByPlaylistIdWithSong(playlistId)
                .stream()
                .map(this::toSongDto)
                .toList();
    }

    /*
     * =========================
     * 작성자 직접 추가 (즉시)
     * POST /playlist/{playlistId}/songs/{songId}
     * =========================
     */
    public void addSongDirectly(Long playlistId, Long songId, String creatorEmail) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (!playlist.getUser().getEmail().equals(creatorEmail)) {
            throw new IllegalStateException("작성자만 곡을 직접 추가할 수 있습니다.");
        }

        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            throw new IllegalArgumentException("이미 수록된 곡입니다.");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("곡을 찾을 수 없습니다."));

        PlaylistSong playlistSong = PlaylistSong.builder()
                .playlist(playlist)
                .song(song)
                .build();

        playlistSongRepository.save(playlistSong);
    }

    /*
     * =========================
     * 수록곡 삭제
     * DELETE /playlist/songs/{playlistSongId}
     * =========================
     */
    public void removeSong(Long playlistSongId) {
        PlaylistSong playlistSong = playlistSongRepository.findById(playlistSongId)
                .orElseThrow(() -> new IllegalArgumentException("수록곡을 찾을 수 없습니다."));

        playlistSongRepository.delete(playlistSong);
    }

    /*
     * =========================
     * 곡 제안 (참여자 → 대기)
     * POST /playlist/{playlistId}/songs/suggest
     * =========================
     */
    public void suggestSong(Long playlistId, Long songId, String suggestorEmail) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        User suggestor = userRepository.findByEmail(suggestorEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            throw new IllegalArgumentException("이미 수록된 곡입니다.");
        }

        if (songSuggestionRepository.existsByPlaylistIdAndSongIdAndSuggestedById(
                playlistId, songId, suggestor.getId())) {
            throw new IllegalArgumentException("이미 제안한 곡입니다.");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("곡을 찾을 수 없습니다."));

        CollaboPlaylistParticipant suggestion = CollaboPlaylistParticipant.builder()
                .playlist(playlist)
                .song(song)
                .suggestedBy(suggestor)
                .build();

        songSuggestionRepository.save(suggestion);
    }

    /*
     * =========================
     * 대기 중인 곡 제안 목록 (작성자만)
     * GET /playlist/{playlistId}/songs/pending
     * =========================
     */
    @Transactional(readOnly = true)
    public List<CollaboPlaylistParticipantDTO> getPendingSuggestions(Long playlistId, String creatorEmail) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (!playlist.getUser().getEmail().equals(creatorEmail)) {
            throw new IllegalStateException("작성자만 대기 목록을 조회할 수 있습니다.");
        }

        return songSuggestionRepository.findByPlaylistIdWithSongAndUser(playlistId)
                .stream()
                .map(this::toSuggestionDto)
                .toList();
    }

    /*
     * =========================
     * 곡 제안 수락 (작성자만 → 수록곡 등록)
     * PUT /playlist/{playlistId}/songs/pending/{suggestionId}/accept
     * =========================
     */
    public void acceptSuggestion(Long playlistId, Long suggestionId, String creatorEmail) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (!playlist.getUser().getEmail().equals(creatorEmail)) {
            throw new IllegalStateException("작성자만 곡 제안을 수락할 수 있습니다.");
        }

        CollaboPlaylistParticipant suggestion = songSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("곡 제안을 찾을 수 없습니다."));

        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, suggestion.getSong().getId())) {
            throw new IllegalArgumentException("이미 수록된 곡입니다.");
        }

        PlaylistSong playlistSong = PlaylistSong.builder()
                .playlist(playlist)
                .song(suggestion.getSong())
                .build();

        playlistSongRepository.save(playlistSong);
        songSuggestionRepository.delete(suggestion);
    }

    /*
     * =========================
     * 곡 제안 거절 (작성자만)
     * DELETE /playlist/{playlistId}/songs/pending/{suggestionId}
     * =========================
     */
    public void rejectSuggestion(Long playlistId, Long suggestionId, String creatorEmail) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (!playlist.getUser().getEmail().equals(creatorEmail)) {
            throw new IllegalStateException("작성자만 곡 제안을 거절할 수 있습니다.");
        }

        CollaboPlaylistParticipant suggestion = songSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("곡 제안을 찾을 수 없습니다."));

        songSuggestionRepository.delete(suggestion);
    }
}

package com.music.music.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.dto.SongDTO;
import com.music.music.entity.Playlist;
import com.music.music.entity.PlaylistSong;
import com.music.music.entity.Song;
import com.music.music.repository.PlaylistRepository;
import com.music.music.repository.PlaylistSongRepository;
import com.music.music.repository.SongRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaylistSongService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;

    // 곡 추가
    public void addSongToPlaylist(Long playlistId, Long songId) {

        // 중복체크
        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            throw new IllegalArgumentException("이 곡은 이미 플레이리스트에 존재합니다.");
        }
        // 엔티티 조회
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("노래를 찾을 수 없습니다."));

        // 엔티티 생성
        PlaylistSong playlistSong = PlaylistSong.builder()
                .playlist(playlist)
                .song(song)
                .build();

        // 저장
        playlistSongRepository.save(playlistSong);
    }

    // 곡 삭제
    public void removeSongFromPlaylist(Long playlistSongId) {
        PlaylistSong ps = playlistSongRepository.findById(playlistSongId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트 곡을 찾을 수 없습니다."));
        playlistSongRepository.delete(ps);
    }

    // 곡 조회
    public List<SongDTO> getSongsByPlaylist(Long playlistId) {
        return playlistSongRepository.findByPlaylistIdWithSong(playlistId)
                .stream()
                .map(ps -> toSongDto(ps.getSong()))
                .toList();
    }

    // Song → DTO
    private SongDTO toSongDto(Song song) {
        return SongDTO.builder()
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
}

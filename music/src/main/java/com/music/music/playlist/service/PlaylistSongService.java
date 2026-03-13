package com.music.music.playlist.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.api.entity.SongDTO;
import com.music.music.api.repository.SongRepository;
import com.music.music.playlist.dto.CollaboSongDto;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.PlaylistSong;
import com.music.music.playlist.entity.PlaylistSongVote;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.playlist.repository.PlaylistSongRepository;
import com.music.music.playlist.repository.PlaylistSongVoteRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
public class PlaylistSongService {

    private static final int MAX_SONGS_PER_USER  = 5;
    private static final int MAX_VOTES_PER_USER  = 3;

    private final PlaylistRepository     playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistSongVoteRepository voteRepository;
    private final SongRepository         songRepository;
    private final UserRepository         userRepository;

    // ─── 일반 플레이리스트 곡 조회 ──────────────────────────────
    @Transactional(readOnly = true)
    public List<SongDTO> getPlaylistSongs(Long playlistId) {
        return playlistSongRepository.findByPlaylistIdWithSong(playlistId)
                .stream()
                .map(this::toSongDto)
                .toList();
    }

    // ─── 공동 플레이리스트 곡 조회 (투표수·등록자 포함) ────────
    @Transactional(readOnly = true)
    public List<CollaboSongDto> getCollaboSongs(Long playlistId, String userEmail) {
        List<PlaylistSong> songs = playlistSongRepository.findByPlaylistIdWithSongAndUser(playlistId);

        // 곡별 투표수 맵 (playlistSongId → count)
        Map<Long, Long> voteCountMap = voteRepository.countVotesByPlaylistId(playlistId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        // 내가 투표한 곡 id 셋
        Set<Long> myVotedIds = (userEmail != null && !userEmail.isBlank())
                ? Set.copyOf(voteRepository.findVotedSongIdsByPlaylistIdAndEmail(playlistId, userEmail))
                : Set.of();

        return songs.stream()
                .map(ps -> CollaboSongDto.builder()
                        .playlistSongId(ps.getId())
                        .songId(ps.getSong().getId())
                        .trackName(ps.getSong().getTrackName())
                        .artistName(ps.getSong().getArtistName())
                        .imgUrl(ps.getSong().getImgUrl())
                        .previewUrl(ps.getSong().getPreviewUrl())
                        .genreName(ps.getSong().getGenreName())
                        .suggestedByName(ps.getSuggestedBy() != null ? ps.getSuggestedBy().getName() : null)
                        .suggestedByEmail(ps.getSuggestedBy() != null ? ps.getSuggestedBy().getEmail() : null)
                        .voteCount(voteCountMap.getOrDefault(ps.getId(), 0L).intValue())
                        .hasVoted(myVotedIds.contains(ps.getId()))
                        .reason(ps.getReason())
                        .build())
                .sorted((a, b) -> b.getVoteCount() - a.getVoteCount())
                .toList();
    }

    // ─── 작성자 직접 추가 ──────────────────────────────────────
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

        playlistSongRepository.save(PlaylistSong.builder()
                .playlist(playlist)
                .song(song)
                .build());
    }

    // ─── 참여자 곡 추가 (최대 5곡) ────────────────────────────
    public void suggestSong(Long playlistId, Long songId, String suggestorEmail, String reason) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("플레이리스트를 찾을 수 없습니다."));

        if (playlist.isDeadlinePassed()) {
            throw new IllegalArgumentException("마감된 플레이리스트에는 곡을 추가할 수 없습니다.");
        }

        User suggestor = userRepository.findByEmail(suggestorEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, songId)) {
            throw new IllegalArgumentException("이미 등록된 곡입니다.");
        }

        int myCount = playlistSongRepository.countByPlaylistIdAndSuggestedById(playlistId, suggestor.getId());
        if (myCount >= MAX_SONGS_PER_USER) {
            throw new IllegalArgumentException("한 플레이리스트에 최대 " + MAX_SONGS_PER_USER + "곡까지 추가할 수 있습니다.");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("곡을 찾을 수 없습니다."));

        try {
            playlistSongRepository.save(PlaylistSong.builder()
                    .playlist(playlist)
                    .song(song)
                    .suggestedBy(suggestor)
                    .reason(reason)
                    .build());
            playlistSongRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 등록된 곡입니다.");
        }
    }

    // ─── 곡 삭제 (작성자 또는 등록자만) ──────────────────────
    public void removeSong(Long playlistSongId, String userEmail) {
        PlaylistSong ps = playlistSongRepository.findById(playlistSongId)
                .orElseThrow(() -> new IllegalArgumentException("수록곡을 찾을 수 없습니다."));

        String creatorEmail   = ps.getPlaylist().getUser().getEmail();
        String suggesterEmail = ps.getSuggestedBy() != null ? ps.getSuggestedBy().getEmail() : null;

        if (!userEmail.equals(creatorEmail) && !userEmail.equals(suggesterEmail)) {
            throw new IllegalStateException("작성자 또는 등록자만 삭제할 수 있습니다.");
        }

        playlistSongRepository.delete(ps);
    }

    // ─── 투표 (최대 3개) ──────────────────────────────────────
    public void vote(Long playlistSongId, String userEmail) {
        PlaylistSong ps = playlistSongRepository.findById(playlistSongId)
                .orElseThrow(() -> new IllegalArgumentException("곡을 찾을 수 없습니다."));

        if (ps.getPlaylist().isDeadlinePassed()) {
            throw new IllegalArgumentException("마감된 플레이리스트에는 투표할 수 없습니다.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (voteRepository.existsByPlaylistSongIdAndUserId(playlistSongId, user.getId())) {
            throw new IllegalArgumentException("이미 투표한 곡입니다.");
        }

        int myVoteCount = voteRepository.countByPlaylistIdAndUserId(ps.getPlaylist().getId(), user.getId());
        if (myVoteCount >= MAX_VOTES_PER_USER) {
            throw new IllegalArgumentException("한 플레이리스트에 최대 " + MAX_VOTES_PER_USER + "곡까지 투표할 수 있습니다.");
        }

        voteRepository.save(PlaylistSongVote.builder()
                .playlistSong(ps)
                .user(user)
                .build());
    }

    // ─── reason 수정 (등록자만) ────────────────────────────────
    public void updateReason(Long playlistSongId, String userEmail, String reason) {
        PlaylistSong ps = playlistSongRepository.findById(playlistSongId)
                .orElseThrow(() -> new IllegalArgumentException("수록곡을 찾을 수 없습니다."));

        if (ps.getSuggestedBy() == null || !ps.getSuggestedBy().getEmail().equals(userEmail)) {
            throw new IllegalStateException("등록자만 추천 이유를 수정할 수 있습니다.");
        }

        ps.updateReason(reason);
    }

    // ─── 투표 취소 ─────────────────────────────────────────────
    public void unvote(Long playlistSongId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        PlaylistSongVote vote = voteRepository
                .findByPlaylistSongIdAndUserId(playlistSongId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("투표 내역이 없습니다."));

        voteRepository.delete(vote);
    }

    // ─── 내부 변환 ─────────────────────────────────────────────
    private SongDTO toSongDto(PlaylistSong ps) {
        Song song = ps.getSong();
        return SongDTO.builder()
                .playlistSongId(ps.getId())
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

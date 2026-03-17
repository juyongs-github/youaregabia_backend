package com.music.music.playlist.scheduler;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.PlaylistSong;
import com.music.music.playlist.repository.PlaylistLikeRepository;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.playlist.repository.PlaylistSongRepository;
import com.music.music.playlist.repository.PlaylistSongVoteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistFinalizeScheduler {

    private static final int TOP_SONGS_LIMIT = 10;
    private static final int MIN_SONGS_REQUIRED = 3;

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistSongVoteRepository voteRepository;
    private final PlaylistLikeRepository playlistLikeRepository;

    // 1분마다 마감 여부 확인
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void finalizeExpiredPlaylists() {
        List<Playlist> expired = playlistRepository.findExpiredCollaborativePlaylists(LocalDateTime.now());

        for (Playlist playlist : expired) {
            try {
                finalizePlaylist(playlist);
            } catch (Exception e) {
                log.error("플레이리스트 마감 처리 실패: playlistId={}", playlist.getId(), e);
            }
        }
    }

    private void finalizePlaylist(Playlist playlist) {
        Long playlistId = playlist.getId();
        List<PlaylistSong> songs = playlistSongRepository.findByPlaylistIdWithSong(playlistId);

        // 3곡 미만이면 플레이리스트 자동 삭제
        if (songs.size() < MIN_SONGS_REQUIRED) {
            voteRepository.deleteByPlaylistId(playlistId);
            playlistLikeRepository.deleteByPlaylistId(playlistId);
            playlistRepository.deleteById(playlistId);
            log.info("수록곡 부족으로 플레이리스트 자동 삭제: playlistId={}, songCount={}", playlistId, songs.size());
            return;
        }

        // 10곡 이하면 순위 처리 불필요
        if (songs.size() <= TOP_SONGS_LIMIT) {
            return;
        }

        // 곡별 투표수 맵
        Map<Long, Long> voteCountMap = voteRepository.countVotesByPlaylistId(playlistId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        // 투표수 내림차순 정렬 후 상위 10곡 id 셋
        Set<Long> topIds = songs.stream()
                .sorted(Comparator.comparingLong(
                        (PlaylistSong ps) -> voteCountMap.getOrDefault(ps.getId(), 0L)).reversed())
                .limit(TOP_SONGS_LIMIT)
                .map(PlaylistSong::getId)
                .collect(Collectors.toSet());

        // 하위 곡들 삭제 (투표 먼저 삭제 후 곡 삭제)
        List<PlaylistSong> toDelete = songs.stream()
                .filter(ps -> !topIds.contains(ps.getId()))
                .toList();

        for (PlaylistSong ps : toDelete) {
            voteRepository.deleteByPlaylistSongId(ps.getId());
            playlistSongRepository.delete(ps);
        }

        log.info("플레이리스트 마감 완료: playlistId={}, 삭제된 곡 수={}", playlistId, toDelete.size());
    }
}

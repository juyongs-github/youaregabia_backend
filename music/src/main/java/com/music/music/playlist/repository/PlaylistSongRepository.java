package com.music.music.playlist.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.playlist.entity.PlaylistSong;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {

    boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

    int countByPlaylistId(Long playlistId);

    // 유저가 해당 플레이리스트에 추가한 곡 수 (최대 5개 제한용)
    int countByPlaylistIdAndSuggestedById(Long playlistId, Long suggestedById);

    // 일반 플레이리스트 곡 조회
    @Query("SELECT ps FROM PlaylistSong ps JOIN FETCH ps.song WHERE ps.playlist.id = :playlistId")
    List<PlaylistSong> findByPlaylistIdWithSong(@Param("playlistId") Long playlistId);

    // 공동 플레이리스트 곡 조회 (등록자 포함, 투표수 내림차순)
    @Query("SELECT ps FROM PlaylistSong ps JOIN FETCH ps.song LEFT JOIN FETCH ps.suggestedBy WHERE ps.playlist.id = :playlistId")
    List<PlaylistSong> findByPlaylistIdWithSongAndUser(@Param("playlistId") Long playlistId);

    // 참여자 수 (곡을 등록한 유니크 유저 수)
    @Query("SELECT COUNT(DISTINCT ps.suggestedBy.id) FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId AND ps.suggestedBy IS NOT NULL")
    int countDistinctSuggestorsByPlaylistId(@Param("playlistId") Long playlistId);

    // 유저 탈퇴 시 suggestedBy 초기화
    @Modifying
    @Query("UPDATE PlaylistSong ps SET ps.suggestedBy = null WHERE ps.suggestedBy.id = :userId")
    void clearSuggestedByUserId(@Param("userId") Long userId);
}

package com.music.music.playlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.playlist.entity.PlaylistSongVote;

public interface PlaylistSongVoteRepository extends JpaRepository<PlaylistSongVote, Long> {

    boolean existsByPlaylistSongIdAndUserId(Long playlistSongId, Long userId);

    Optional<PlaylistSongVote> findByPlaylistSongIdAndUserId(Long playlistSongId, Long userId);

    // 유저가 해당 플레이리스트에 투표한 총 수 (최대 3개 제한용)
    @Query("SELECT COUNT(v) FROM PlaylistSongVote v WHERE v.playlistSong.playlist.id = :playlistId AND v.user.id = :userId")
    int countByPlaylistIdAndUserId(@Param("playlistId") Long playlistId, @Param("userId") Long userId);

    // 플레이리스트 내 곡별 투표수 (playlistSongId → count)
    @Query("SELECT v.playlistSong.id, COUNT(v) FROM PlaylistSongVote v WHERE v.playlistSong.playlist.id = :playlistId GROUP BY v.playlistSong.id")
    List<Object[]> countVotesByPlaylistId(@Param("playlistId") Long playlistId);

    // 유저가 투표한 곡 id 목록
    @Query("SELECT v.playlistSong.id FROM PlaylistSongVote v WHERE v.playlistSong.playlist.id = :playlistId AND v.user.email = :email")
    List<Long> findVotedSongIdsByPlaylistIdAndEmail(@Param("playlistId") Long playlistId, @Param("email") String email);

    void deleteByPlaylistSongId(Long playlistSongId);

    // 유저 탈퇴 시 투표 데이터 삭제
    void deleteByUserId(Long userId);

    // 플레이리스트 삭제 시 투표 데이터 삭제
    @Query("DELETE FROM PlaylistSongVote v WHERE v.playlistSong.playlist.id = :playlistId")
    @org.springframework.data.jpa.repository.Modifying
    void deleteByPlaylistId(@Param("playlistId") Long playlistId);
}

package com.music.music.playlist.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.entity.constant.PlaylistType;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    boolean existsByUserIdAndTitle(Long userId, String title);

    List<Playlist> findByType(PlaylistType type);

    @Query("SELECT p FROM Playlist p JOIN FETCH p.user WHERE p.type = :type ORDER BY p.createdAt DESC")
    List<Playlist> findByTypeWithUser(PlaylistType type);

    // 마감 시간이 지난 공동 플레이리스트 조회 (스케줄러용)
    @Query("SELECT p FROM Playlist p WHERE p.type = 'COLLABORATIVE' AND p.deadline IS NOT NULL AND p.deadline < :now")
    List<Playlist> findExpiredCollaborativePlaylists(@Param("now") LocalDateTime now);

    // 전체 조회 (내 플레이리스트만)
    List<Playlist> findAllByUserIdAndType(Long userId, PlaylistType type);

    // 상세 조회
    Optional<Playlist> findByIdAndUserId(Long playlistId, Long userId);

    void deleteByUserId(Long userId);
}

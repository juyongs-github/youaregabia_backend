package com.music.music.playlist.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.playlist.entity.PlaylistLike;

public interface PlaylistLikeRepository extends JpaRepository<PlaylistLike, Long> {

    boolean existsByPlaylistIdAndUserId(Long playlistId, Long userId);

    Optional<PlaylistLike> findByPlaylistIdAndUserId(Long playlistId, Long userId);

    int countByPlaylistId(Long playlistId);

    // 유저가 좋아요한 플레이리스트 id 목록 (여러 플레이리스트 한 번에 조회용)
    @Query("SELECT l.playlist.id FROM PlaylistLike l WHERE l.playlist.id IN :playlistIds AND l.user.email = :email")
    java.util.List<Long> findLikedPlaylistIdsByEmailAndPlaylistIds(
            @Param("playlistIds") java.util.List<Long> playlistIds,
            @Param("email") String email);

    void deleteByUserId(Long userId);

    void deleteByPlaylistId(Long playlistId);
}

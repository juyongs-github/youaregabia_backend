package com.music.music.playlist.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.playlist.entity.PlaylistImport;

public interface PlaylistImportRepository extends JpaRepository<PlaylistImport, Long> {

    boolean existsByPlaylistIdAndUserId(Long playlistId, Long userId);

    // 여러 플레이리스트에 대해 유저가 import한 id 목록 (목록 조회용)
    @Query("SELECT i.playlist.id FROM PlaylistImport i WHERE i.playlist.id IN :playlistIds AND i.user.email = :email")
    List<Long> findImportedPlaylistIdsByEmailAndPlaylistIds(
            @Param("playlistIds") List<Long> playlistIds,
            @Param("email") String email);

    void deleteByPlaylistId(Long playlistId);
}

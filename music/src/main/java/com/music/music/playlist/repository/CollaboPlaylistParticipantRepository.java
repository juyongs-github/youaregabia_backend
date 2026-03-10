package com.music.music.playlist.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.playlist.entity.CollaboPlaylistParticipant;

public interface CollaboPlaylistParticipantRepository extends JpaRepository<CollaboPlaylistParticipant, Long> {

    @Query("select p from CollaboPlaylistParticipant p join fetch p.song join fetch p.suggestedBy where p.playlist.id = :playlistId")
    List<CollaboPlaylistParticipant> findByPlaylistIdWithSongAndUser(Long playlistId);

    boolean existsByPlaylistIdAndSongIdAndSuggestedById(Long playlistId, Long songId, Long suggestedById);
}

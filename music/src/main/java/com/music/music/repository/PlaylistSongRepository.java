package com.music.music.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistSongRepository extends JpaRepository<PlaylistRepository, Long> {

}

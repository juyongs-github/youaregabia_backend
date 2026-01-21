package com.music.music.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.entity.Playlist;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

}

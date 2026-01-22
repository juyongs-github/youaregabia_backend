package com.music.music.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.music.music.entity.Playlist;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

}

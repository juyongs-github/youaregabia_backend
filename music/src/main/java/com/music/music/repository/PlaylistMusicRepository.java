package com.music.music.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import com.music.music.entity.PlaylistMusic;

public interface PlaylistMusicRepository extends JpaRepository<PlaylistMusic,Long>{
    
}

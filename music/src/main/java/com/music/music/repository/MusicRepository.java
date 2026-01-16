package com.music.music.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import com.music.music.entity.Music;

public interface MusicRepository extends JpaRepository<Music,Long>{
    
}

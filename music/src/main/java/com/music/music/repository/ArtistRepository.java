package com.music.music.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.entity.Artist;

public interface ArtistRepository extends JpaRepository<Artist,Long>{
    
}

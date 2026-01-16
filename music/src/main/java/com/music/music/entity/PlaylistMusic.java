package com.music.music.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class PlaylistMusic extends BaseEntity{
    
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional =false)
    @JoinColumn(name = "id")
    private Music musicId;

    @ManyToOne(fetch=FetchType.LAZY, optional =false)
    @JoinColumn(name = "id")
    private Playlist playlistId;
    
}

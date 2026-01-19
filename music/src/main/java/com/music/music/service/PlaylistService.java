package com.music.music.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.dto.InPlaylistDTO;
import com.music.music.dto.OutPlaylistDTO;
import com.music.music.entity.Playlist;
import com.music.music.repository.PlaylistRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Transactional
@Service
@Log4j2
@RequiredArgsConstructor
public class PlaylistService {
    
    private final PlaylistRepository playlistRepository;
    // Create
    public Long CreatePlaylist(InputPlaylistDTO dto) {
        Playlist playlist = Playlist.builder().title(dto.getTitle()).build();

        return playlistRepository.save(playlist).getId();
    }
    

    
}

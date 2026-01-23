package com.music.music.api.dto;

import java.util.List;

import com.music.music.api.entity.SongDTO;

import lombok.Data;

@Data
public class ItunesSearchResponse {
    private List<SongDTO> results;
}
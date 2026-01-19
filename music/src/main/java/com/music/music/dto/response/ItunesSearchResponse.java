package com.music.music.dto.response;

import java.util.List;

import com.music.music.dto.SongDTO;

import lombok.Data;

@Data
public class ItunesSearchResponse {
    private List<SongDTO> results;
}
package com.music.music.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SimilarArtistResponse {
    @JsonProperty("similarartists")
    private SimilarArtists SimilarArtists;

    @Data
    public static class SimilarArtists {
        private List<ArtistDTO> artist;
    }
}

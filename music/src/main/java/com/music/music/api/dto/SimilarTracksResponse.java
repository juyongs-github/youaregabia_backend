package com.music.music.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SimilarTracksResponse {
    @JsonProperty("similartracks")
    private SimilarTracks similarTracks;

    @Data
    public static class SimilarTracks {
        private List<TrackDTO> track;
    }
}

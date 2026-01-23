package com.music.music.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TrackDTO {
    private String name;
    private Double match; // 유사도 (0 ~ 1)
    @JsonProperty("artist")
    private ArtistDTO artistDto;
}

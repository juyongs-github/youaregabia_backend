package com.music.music.api.dto;

import lombok.Data;

@Data
public class ArtistDTO {
    private String name;
    private Double match; // 유사도 (0 ~ 1)
}

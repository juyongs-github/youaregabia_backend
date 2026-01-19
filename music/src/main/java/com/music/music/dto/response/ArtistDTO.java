package com.music.music.dto.response;

import lombok.Data;

@Data
public class ArtistDTO {
    private String name;
    private Double match; // 유사도 (0 ~ 1)
}

package com.music.music.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MusicDTO {

    
    private Long id;

    private String title;
    private String artist;
    
    private String ImageUrl;

    private Integer durationMs;

}

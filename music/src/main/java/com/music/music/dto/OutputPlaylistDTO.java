package com.music.music.dto;

import java.util.List;

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

// 노출용
public class OutputPlaylistDTO {

    private Long id;

    private String title;
    // private String imageUrl;

    private List<MusicDTO> musics;


}

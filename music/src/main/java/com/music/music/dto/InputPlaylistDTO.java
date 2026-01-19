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

// 입력용
public class InputPlaylistDTO {
    private Long id;

    private String title;
    // private String imageUrl;

    private List<MusicDTO> musics;

}

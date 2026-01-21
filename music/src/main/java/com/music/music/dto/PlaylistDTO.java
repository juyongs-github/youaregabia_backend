package com.music.music.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistDTO {

    private Long id;

    private String title;

    private String description;

    private String imageUrl;

    // 플레이리스트에 속한 곡 수
    private int songCount;
    // 플레이리스트 곡 목록
    private List<SongDTO> songs;

}

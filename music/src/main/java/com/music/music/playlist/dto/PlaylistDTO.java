package com.music.music.playlist.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.music.music.api.entity.SongDTO;
import com.music.music.playlist.entity.constant.PlaylistType;

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

    private PlaylistType type;

    // 플레이리스트에 속한 곡 수
    private int songCount;
    // 플레이리스트 곡 목록
    private List<SongDTO> songs;
    // 마감 시간 (공동 플레이리스트인 경우)
    private LocalDateTime deadline;
}

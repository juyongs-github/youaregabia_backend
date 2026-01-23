package com.music.music.dto;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SongDTO {
    @SerializedName("trackId")
    private Long id;

    private String trackName;
    private String artistName;
    private String previewUrl; // 곡 미리듣기

    @SerializedName("artworkUrl100")
    private String imgUrl; // 곡 이미지

    private String releaseDate; // 곡 발매일

    @SerializedName("trackTimeMillis")
    private Long durationMs; // 곡 플레이 시간

    @SerializedName("primaryGenreName")
    private String genreName; // 곡 장르
}

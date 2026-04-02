package com.music.music.playlist.dto;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongDTO {
    @SerializedName("trackId")
    private Long id;

    private String trackName;
    private String artistName;
    private String previewUrl;

    @SerializedName("artworkUrl100")
    private String imgUrl;

    private String releaseDate;

    @SerializedName("trackTimeMillis")
    private Long durationMs;

    @SerializedName("primaryGenreName")
    private String genreName;

    private Long playlistSongId;

    // ✅ Gson 파싱 후 해상도 업그레이드 (100x100 → 600x600)
    public String getImgUrl() {
        if (imgUrl != null && imgUrl.contains("100x100bb")) {
            return imgUrl.replace("100x100bb", "600x600bb");
        }
        return imgUrl;
    }
}
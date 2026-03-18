package com.music.music.api.dto;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.music.music.playlist.dto.SongDTO;

import lombok.Data;

@Data
public class SpotifySearchResponse {

    private TrackPage tracks;

    @Data
    public static class TrackPage {
        private List<SpotifyTrack> items;
        private int total;
        private int limit;
        private int offset;
    }

    @Data
    public static class SpotifyTrack {
        private String id;
        private String name; // → trackName
        private List<SpotifyArtist> artists; // → artistName
        private SpotifyAlbum album; // → imgUrl, releaseDate
        @SerializedName("duration_ms")
        private Long durationMs;
        @SerializedName("preview_url")
        private String previewUrl;

        // SpotifyTrack → SongDTO 변환
        public SongDTO toSongDTO() {
            return SongDTO.builder()
                    .trackName(this.name)
                    .artistName(artists != null && !artists.isEmpty()
                            ? artists.get(0).getName()
                            : "")
                    .imgUrl(album != null && album.getImages() != null && !album.getImages().isEmpty()
                            ? album.getImages().get(0).getUrl()
                            : "")
                    .releaseDate(album != null ? album.getReleaseDate() : "")
                    .durationMs(this.durationMs)
                    .previewUrl(this.previewUrl)
                    .genreName("") // Spotify Search API는 장르 미제공
                    .build();
        }
    }

    @Data
    public static class SpotifyArtist {
        private String id;
        private String name;
    }

    @Data
    public static class SpotifyAlbum {
        private String id;
        private String name;
        private List<SpotifyImage> images;
        @SerializedName("release_date")
        private String releaseDate;
    }

    @Data
    public static class SpotifyImage {
        private String url;
        private int width;
        private int height;
    }
}
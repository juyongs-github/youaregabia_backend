package com.music.music.entity;

public class Playlist {
    
}
    @Id
    private Long id;

    private String trackName;
    private String artistName;
    private String previewUrl; // 곡 미리듣기
    private String imgUrl; // 곡 이미지
    private String releaseDate; // 곡 발매일
    private Long durationMs; // 곡 플레이 시간
    private String genreName; // 곡 장르

    @CreatedDate
    private LocalDateTime createdAt;
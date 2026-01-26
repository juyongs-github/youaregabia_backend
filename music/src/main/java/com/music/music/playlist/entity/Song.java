package com.music.music.playlist.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@EntityListeners(value = AuditingEntityListener.class)
@Table(name = "song")
@Data
@Entity
public class Song {
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
}

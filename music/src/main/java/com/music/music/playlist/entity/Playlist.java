package com.music.music.playlist.entity;

import java.util.ArrayList;
import java.util.List;

import com.music.music.common.entity.BaseEntity;
import com.music.music.playlist.entity.constant.PlaylistType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString(exclude = "playlistSongs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Playlist extends BaseEntity {

    // 공통

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaylistType type;

    @Column(nullable = false)
    private String title;

    private String description;

    @OneToMany(mappedBy = "playlist", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistSong> playlistSongs = new ArrayList<>();

    private String imageUrl;

    // 선택사항 (공동 플레이리스트 제작 시 필요)
    private String genre;

    // 곡 추가 메서드
    public void addSong(Song song) {
        PlaylistSong playlistSong = PlaylistSong.builder()
                .playlist(this)
                .song(song)
                .build();

        playlistSongs.add(playlistSong);
    }

    // 수정 메서드
    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
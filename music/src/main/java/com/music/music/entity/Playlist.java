package com.music.music.entity;

import java.util.ArrayList;
import java.util.List;

import com.music.music.entity.constant.PlaylistType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Playlist extends BaseEntity {

    // 공통

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private PlaylistType type;

    @Column(nullable = false)
    private String title;

    private String description;

    @OneToMany(mappedBy = "playlist", fetch = FetchType.LAZY)
    private List<PlaylistSong> playlistSongs = new ArrayList<>();

    // 이미지 추가
    private String imageUrl;
    // 선택사항 (공동 플레이리스트 제작 시 필요)
    private String genre;

    // 플레이리스트에서 노래를 검색해서 추가하는 메서드
    public void addSong(Song song) {
        PlaylistSong playlistSong = PlaylistSong.builder()
                .playlist(this)
                .song(song)
                .build();
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
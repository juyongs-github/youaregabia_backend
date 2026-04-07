package com.music.music.review.entity;

import com.music.music.board.common.entity.BaseEntity;
import com.music.music.playlist.entity.Playlist;
import com.music.music.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString(exclude = {"playlist", "user"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @Builder.Default
    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewSong> reviewSongs = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    // 플레이리스트 삭제 시 연결 해제 (리뷰는 유지)
    public void detachPlaylist() {
        this.playlist = null;
    }

    // 수정 메서드
    public void updateContent(String content) {
        this.content = content;
    }

    public void updateRating(Integer rating) {
        this.rating = rating;
    }
}
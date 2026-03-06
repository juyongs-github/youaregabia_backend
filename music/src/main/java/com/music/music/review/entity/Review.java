package com.music.music.review.entity;

import com.music.music.board.common.entity.BaseEntity;
import com.music.music.playlist.entity.Playlist;
import com.music.music.user.entitiy.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    // 수정 메서드
    public void updateContent(String content) {
        this.content = content;
    }

    public void updateRating(Integer rating) {
        this.rating = rating;
    }
}
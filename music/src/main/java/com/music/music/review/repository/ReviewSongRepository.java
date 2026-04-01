package com.music.music.review.repository;

import com.music.music.review.entity.ReviewSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewSongRepository extends JpaRepository<ReviewSong, Long> {

    @Query("SELECT rs FROM ReviewSong rs JOIN FETCH rs.song WHERE rs.review.id = :reviewId")
    List<ReviewSong> findByReviewIdWithSong(@Param("reviewId") Long reviewId);

    void deleteByReviewId(Long reviewId);
}

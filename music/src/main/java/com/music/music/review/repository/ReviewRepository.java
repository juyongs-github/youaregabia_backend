package com.music.music.review.repository;

import com.music.music.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPlaylistId(Long playlistId);

    List<Review> findByUserId(Long userId);
}
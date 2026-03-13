package com.music.music.review.service;

import com.music.music.review.dto.ReviewDto;
import com.music.music.review.entity.Review;
import com.music.music.review.repository.ReviewRepository;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private UserRepository userRepository;

    private ReviewDto toDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .playlistId(review.getPlaylist().getId())
                .playlistTitle(review.getPlaylist().getTitle())
                .imageUrl(review.getPlaylist().getImageUrl())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .userEmail(review.getUser().getEmail())
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    @Transactional
    public ReviewDto createReview(Long playlistId, String userEmail, String content, Integer rating) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("해당 플레이리스트가 없습니다."));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        Review review = Review.builder()
                .playlist(playlist)
                .user(user)
                .content(content)
                .rating(rating)
                .build();

        Review savedReview = reviewRepository.save(review);

        return toDto(savedReview);
    }

    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<ReviewDto> getReviewsByPlaylist(Long playlistId) {
        return reviewRepository.findByPlaylistId(playlistId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ReviewDto> getReviewsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        return reviewRepository.findByUserId(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ReviewDto updateReview(Long reviewId, String content, Integer rating) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 없습니다."));

        review.updateContent(content);
        review.updateRating(rating);

        return toDto(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 없습니다."));

        reviewRepository.delete(review);
    }
}
package com.music.music.review.service;

import com.music.music.review.dto.ReviewDto;
import com.music.music.review.entity.Review;
import com.music.music.review.repository.ReviewRepository;
import com.music.music.playlist.entity.Playlist;
import com.music.music.playlist.repository.PlaylistRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import org.modelmapper.ModelMapper;
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

    @Autowired
    private ModelMapper modelMapper;

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

        return modelMapper.map(savedReview, ReviewDto.class);
    }

    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(review -> modelMapper.map(review, ReviewDto.class))
                .toList();
    }

    public List<ReviewDto> getReviewsByPlaylist(Long playlistId) {
        List<Review> reviews = reviewRepository.findByPlaylistId(playlistId);

        return reviews.stream()
                .map(review -> modelMapper.map(review, ReviewDto.class))
                .toList();
    }

    public List<ReviewDto> getReviewsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 없습니다."));

        List<Review> reviews = reviewRepository.findByUserId(user.getId());

        return reviews.stream()
                .map(review -> modelMapper.map(review, ReviewDto.class))
                .toList();
    }

    @Transactional
    public ReviewDto updateReview(Long reviewId, String content, Integer rating) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 없습니다."));

        review.updateContent(content);
        review.updateRating(rating);

        return modelMapper.map(review, ReviewDto.class);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰가 없습니다."));

        reviewRepository.delete(review);
    }
}
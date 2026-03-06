package com.music.music.review.controller;

import com.music.music.review.dto.ReviewDto;
import com.music.music.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(@RequestBody CreateReviewRequest request) {
        ReviewDto review = reviewService.createReview(request.getPlaylistId(), request.getUserEmail(), request.getContent(), request.getRating());
        return ResponseEntity.status(201).body(review);
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByPlaylist(@PathVariable Long playlistId) {
        List<ReviewDto> reviews = reviewService.getReviewsByPlaylist(playlistId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{userEmail}")
    public ResponseEntity<List<ReviewDto>> getReviewsByUser(@PathVariable String userEmail) {
        List<ReviewDto> reviews = reviewService.getReviewsByUser(userEmail);
        return ResponseEntity.ok(reviews);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable Long reviewId,
            @RequestBody UpdateReviewRequest request) {
        ReviewDto review = reviewService.updateReview(reviewId, request.getContent(), request.getRating());
        return ResponseEntity.ok(review);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    // Inner classes for request DTOs
    public static class CreateReviewRequest {
        private Long playlistId;
        private String userEmail;
        private Integer rating;
        private String content;

        public Long getPlaylistId() { return playlistId; }
        public void setPlaylistId(Long playlistId) { this.playlistId = playlistId; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
    }

    public static class UpdateReviewRequest {
        private String content;
        private Integer rating;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
    }
}
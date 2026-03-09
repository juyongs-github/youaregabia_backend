package com.music.music.review.controller;

import com.music.music.review.dto.ReviewDto;
import com.music.music.review.dto.ReviewRequestDto;
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
    public ResponseEntity<ReviewDto> createReview(@RequestBody ReviewRequestDto request) {
        ReviewDto review = reviewService.createReview(request.getPlaylistId(), request.getUserEmail(), request.getContent(), request.getRating());
        return ResponseEntity.status(201).body(review);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        List<ReviewDto> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/playlist/{playlistId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByPlaylist(@PathVariable(name = "playlistId") Long playlistId) {
        List<ReviewDto> reviews = reviewService.getReviewsByPlaylist(playlistId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<List<ReviewDto>> getReviewsByUser(@PathVariable(name = "email") String email) {
        List<ReviewDto> reviews = reviewService.getReviewsByUser(email);
        return ResponseEntity.ok(reviews);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequestDto request) {
        Integer rating = null;
        if (request.getRating() != null) {
            rating = request.getRating();
        }
        ReviewDto review = reviewService.updateReview(reviewId, request.getContent(), rating);
        return ResponseEntity.ok(review);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}

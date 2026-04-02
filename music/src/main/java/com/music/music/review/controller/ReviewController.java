package com.music.music.review.controller;

import com.music.music.playlist.dto.SongDTO;
import com.music.music.review.dto.ReviewDto;
import com.music.music.review.dto.ReviewRequestDto;
import com.music.music.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
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
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/playlist/{playlistId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByPlaylist(@PathVariable(name = "playlistId") Long playlistId) {
        return ResponseEntity.ok(reviewService.getReviewsByPlaylist(playlistId));
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<List<ReviewDto>> getReviewsByUser(@PathVariable(name = "email") String email) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(email));
    }

    @GetMapping("/{reviewId}/songs")
    public ResponseEntity<List<SongDTO>> getReviewSongs(@PathVariable(name = "reviewId") Long reviewId) {
        return ResponseEntity.ok(reviewService.getReviewSongs(reviewId));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable(name = "reviewId") Long reviewId,
            @RequestBody ReviewRequestDto request) {
        Integer rating = null;
        if (request.getRating() != null) {
            rating = request.getRating();
        }
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request.getContent(), rating));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable(name = "reviewId") Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}

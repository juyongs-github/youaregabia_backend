package com.music.music.review.controller;

import com.music.music.review.service.ReviewService;
import com.music.music.review.dto.ReviewDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateReview() throws Exception {
        ReviewDto reviewDto = ReviewDto.builder()
                .id(1L)
                .playlistId(1L)
                .userId(1L)
                .content("한줄평 테스트")
                .rating(5)
                .build();

        when(reviewService.createReview(eq(1L), eq("test@example.com"), any(), any())).thenReturn(reviewDto);

        ReviewController.CreateReviewRequest request = new ReviewController.CreateReviewRequest();
        request.setContent("Great playlist!");
        request.setRating(5);

        mockMvc.perform(post("/review")
                .param("playlistId", "1")
                .param("userEmail", "test@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("한줄평 테스트"));
    }

    @Test
    public void testGetReviewsByPlaylist() throws Exception {
        ReviewDto reviewDto = ReviewDto.builder()
                .id(1L)
                .playlistId(1L)
                .userId(1L)
                .content("한줄평 테스트")
                .rating(5)
                .build();

        List<ReviewDto> reviews = Arrays.asList(reviewDto);

        when(reviewService.getReviewsByPlaylist(1L)).thenReturn(reviews);

        mockMvc.perform(get("/review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("한줄평 테스트"));
    }
}
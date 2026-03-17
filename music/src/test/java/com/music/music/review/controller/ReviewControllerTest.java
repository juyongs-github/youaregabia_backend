package com.music.music.review.controller;

import com.music.music.review.service.ReviewService;
import com.music.music.review.dto.ReviewDto;
import com.music.music.review.dto.ReviewRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled
@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
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

                ReviewRequestDto request = ReviewRequestDto.builder()
                                .playlistId(1L)
                                .userEmail("test@example.com")
                                .content("한줄평 테스트")
                                .rating(5)
                                .build();

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

                mockMvc.perform(get("/review/playlist/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].content").value("한줄평 테스트"));
        }
}
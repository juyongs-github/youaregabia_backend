package com.music.music.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.dto.SongDTO;
import com.music.music.service.MusicApiService;

@RestController
public class MusicApiController {
    private Logger logger = LoggerFactory.getLogger(MusicApiController.class);

    @Autowired
    MusicApiService musicApiService;

    @GetMapping("/api/init")
    public ResponseEntity<String> saveInitialSongInfo() {
        try {
            musicApiService.saveInitialSongInfo();
            return ResponseEntity.ok("초기 곡 정보 DB INSERT 완료");
        } catch (Exception e) {
            logger.error("[saveInitialSongInfo] 초기 곡 정보 DB INSERT 실패 - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("초기 곡 정보 DB INSERT 실패");
        }
    }

    @GetMapping("/api/recommend")
    public List<SongDTO> getRecommendSongList(
        @RequestParam String trackName, 
        @RequestParam String artistName) {
        return musicApiService.getRecommendSongList(trackName, artistName);
    }

    @GetMapping("/api/search")
    public List<SongDTO> getSearchSongList(
        @RequestParam("q") String query) {
        return musicApiService.getSearchSongList(query);
    }
}

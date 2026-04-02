package com.music.music.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.music.music.api.service.MusicApiService;
import com.music.music.playlist.dto.SongDTO;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.repository.SongRepository;
import com.music.music.recommendation.dto.RecommendedSongDto;
import com.music.music.recommendation.service.RecommendationOrchestrator;
import com.music.music.recommendation.service.VectorSearchService;

@RestController
public class MusicApiController {
  private Logger logger = LoggerFactory.getLogger(MusicApiController.class);

  @Autowired
  MusicApiService musicApiService;

  @Autowired
  RecommendationOrchestrator recommendationOrchestrator;

  @Autowired
  SongRepository songRepository;

  @Autowired
  VectorSearchService vectorSearchService;

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
  public List<RecommendedSongDto> getRecommendSongList(
      @RequestParam("trackName") String trackName,
      @RequestParam("artistName") String artistName,
      @RequestParam(value = "genre", required = false) String genre) {
    return recommendationOrchestrator.recommend(trackName, artistName, genre, 15);
  }

  @GetMapping("/api/vector/index-all")
  public ResponseEntity<String> indexAllSongs() {
    try {
      List<Song> all = songRepository.findAll();
      if (all.isEmpty()) return ResponseEntity.ok("인덱싱할 곡이 없습니다.");
      vectorSearchService.indexSongs(all);
      return ResponseEntity.ok("FAISS 인덱싱 완료: " + all.size() + "곡");
    } catch (Exception e) {
      logger.error("[indexAllSongs] 실패: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("인덱싱 실패: " + e.getMessage());
    }
  }

  @GetMapping("/api/search")
  public List<SongDTO> getSearchSongList(
      @RequestParam("q") String query) {
    return musicApiService.getSearchSongList(query);
  }

  @GetMapping("/api/random")
  public ResponseEntity<SongDTO> getRandomSong() {
    Song song = songRepository.findRandomSong();
    SongDTO dto = SongDTO.builder()
        .id(song.getId())
        .trackName(song.getTrackName())
        .artistName(song.getArtistName())
        .previewUrl(song.getPreviewUrl())
        .imgUrl(song.getImgUrl())
        .releaseDate(song.getReleaseDate())
        .durationMs(song.getDurationMs())
        .genreName(song.getGenreName())
        .build();

    return ResponseEntity.ok(dto);
  }

  @GetMapping("/api/randoms")
  public ResponseEntity<List<SongDTO>> getRandomSongs(@RequestParam int limit) {
    List<Song> songs = songRepository.findRandomSongs(limit);
    List<SongDTO> dtos = new ArrayList<>();
    for (Song song : songs) {
      dtos.add(SongDTO.builder()
          .id(song.getId())
          .trackName(song.getTrackName())
          .artistName(song.getArtistName())
          .previewUrl(song.getPreviewUrl())
          .imgUrl(song.getImgUrl())
          .releaseDate(song.getReleaseDate())
          .durationMs(song.getDurationMs())
          .genreName(song.getGenreName())
          .build());
    }
    return ResponseEntity.ok(dtos);
  }

}

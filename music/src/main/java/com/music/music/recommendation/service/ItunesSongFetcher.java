package com.music.music.recommendation.service;

import com.google.gson.Gson;
import com.music.music.api.dto.ItunesSearchResponse;
import com.music.music.playlist.dto.SongDTO;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.repository.SongRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * iTunes Search API를 통해 곡 메타데이터를 조회하고 DB에 저장하는 공유 서비스.
 * MusicApiService와 중복 없이 RecommendationOrchestrator, YtMusicService에서 재사용.
 */
@Service
public class ItunesSongFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ItunesSongFetcher.class);
    private final Gson gson = new Gson();

    private final RestClient itunesRestClient;
    private final SongRepository songRepository;
    private final ModelMapper modelMapper;

    public ItunesSongFetcher(
            @Qualifier("itunesRestClient") RestClient itunesRestClient,
            SongRepository songRepository,
            ModelMapper modelMapper) {
        this.itunesRestClient = itunesRestClient;
        this.songRepository = songRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * iTunes에서 곡을 검색하고 DB에 없으면 저장 후 SongDTO 반환.
     * 검색 실패 시 Optional.empty() 반환.
     */
    @Transactional
    public Optional<SongDTO> fetchAndSave(String title, String artist) {
        try {
            // 1. DB에서 먼저 검색 (곡명+아티스트 → 곡명만)
            Optional<Song> dbSong = songRepository.findByTrackNameAndArtistNameLike(title, artist);
            if (dbSong.isEmpty()) {
                dbSong = songRepository.findByTrackNameLike(title);
            }
            if (dbSong.isPresent()) {
                return Optional.of(modelMapper.map(dbSong.get(), SongDTO.class));
            }

            // 2. DB에 없으면 iTunes API 호출
            String term = title + " " + artist;
            String body = itunesRestClient.get()
                    .uri(url -> url.path("/search")
                            .queryParam("term", term)
                            .queryParam("country", "KR")
                            .queryParam("media", "music")
                            .queryParam("entity", "song")
                            .queryParam("attribute", "mixTerm")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(String.class);

            ItunesSearchResponse response = gson.fromJson(body, ItunesSearchResponse.class);
            if (response == null || response.getResults().isEmpty()) return Optional.empty();

            SongDTO dto = response.getResults().get(0);
            Song song = modelMapper.map(dto, Song.class);
            songRepository.findById(dto.getId()).orElseGet(() -> songRepository.save(song));

            return Optional.of(dto);
        } catch (Exception e) {
            logger.warn("[ItunesSongFetcher] 조회 실패 - {} {}: {}", title, artist, e.getMessage());
            return Optional.empty();
        }
    }
}

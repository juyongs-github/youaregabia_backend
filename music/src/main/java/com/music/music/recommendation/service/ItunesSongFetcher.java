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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Optional;

/**
 * iTunes Search API를 통해 곡 메타데이터를 조회하고 DB에 저장하는 공유 서비스.
 * MusicApiService와 중복 없이 RecommendationOrchestrator에서 재사용.
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
    @Cacheable(value = "itunesSongs", key = "#title + '_' + #artist", unless = "#result == null")
    public Optional<SongDTO> fetchAndSave(String title, String artist) {
        try {
            // DB 조회와 외부 API 호출을 분리해 커넥션을 오래 점유하지 않도록 한다.
            Optional<Song> dbSong = findExistingSong(title, artist);
            if (dbSong.isPresent()) {
                return Optional.of(modelMapper.map(dbSong.get(), SongDTO.class));
            }

            // DB에 없으면 iTunes API 호출
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
            if (!isLikelyMatch(title, artist, dto.getTrackName(), dto.getArtistName())) {
                logger.info("[ItunesSongFetcher] 매칭 제외 - expected=\"{} - {}\", actual=\"{} - {}\"",
                        title, artist, dto.getTrackName(), dto.getArtistName());
                return Optional.empty();
            }
            return saveIfMissing(dto);
        } catch (Exception e) {
            logger.warn("[ItunesSongFetcher] 조회 실패 - {} {}: {}", title, artist, e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public Optional<Song> findExistingSong(String title, String artist) {
        Optional<Song> dbSong = songRepository.findByTrackNameAndArtistNameLike(title, artist);
        if (dbSong.isEmpty()) {
            dbSong = songRepository.findByTrackNameLike(title)
                    .filter(song -> isLikelyMatch(title, artist, song.getTrackName(), song.getArtistName()));
        }
        return dbSong;
    }

    @Transactional
    protected Optional<SongDTO> saveIfMissing(SongDTO dto) {
        Song saved = songRepository.findById(dto.getId())
                .orElseGet(() -> songRepository.save(modelMapper.map(dto, Song.class)));
        return Optional.of(modelMapper.map(saved, SongDTO.class));
    }

    private boolean isStrictTitleMatch(String expectedTitle, String actualTitle) {
        String expected = normalizeTitle(expectedTitle);
        String actual = normalizeTitle(actualTitle);

        if (expected.isBlank() || actual.isBlank()) {
            return false;
        }

        if (expected.equals(actual)) {
            return true;
        }

        if (expected.length() < 5 || actual.length() < 5) {
            return false;
        }

        return expected.contains(actual) || actual.contains(expected);
    }

    private String normalizeTitle(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\(.*?\\)|\\[.*?\\]", " ")
                .replaceAll("(?i)feat\\.?|ft\\.?|ver\\.?|version|live|remix|inst\\.?|instrumental", " ")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .trim();
    }

    private boolean isLikelyMatch(String expectedTitle, String expectedArtist, String actualTitle, String actualArtist) {
        if (!isStrictTitleMatch(expectedTitle, actualTitle)) {
            return false;
        }
        return isLikelySameArtist(expectedArtist, actualArtist);
    }

    private boolean isLikelySameArtist(String expectedArtist, String actualArtist) {
        String expected = normalizeArtist(expectedArtist);
        String actual = normalizeArtist(actualArtist);

        if (expected.isBlank() || actual.isBlank()) {
            return true;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (expected.length() >= 3 && actual.contains(expected)) {
            return true;
        }
        return actual.length() >= 3 && expected.contains(actual);
    }

    private String normalizeArtist(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("(?i)feat\\.?|ft\\.?|with|x|&", " ")
                .replaceAll("\\(.*?\\)|\\[.*?\\]", " ")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .trim();
    }
}

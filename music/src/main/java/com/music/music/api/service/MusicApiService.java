package com.music.music.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.google.gson.Gson;
import com.music.music.api.dto.ArtistDTO;
import com.music.music.api.dto.ItunesSearchResponse;
import com.music.music.api.dto.SpotifySearchResponse;
import com.music.music.api.dto.SimilarArtistResponse;
import com.music.music.api.dto.SimilarTracksResponse;
import com.music.music.api.dto.TrackDTO;
import com.music.music.playlist.dto.SongDTO;
import com.music.music.playlist.entity.Song;
import com.music.music.playlist.repository.SongRepository;

@Service
public class MusicApiService {
    private final Logger logger = LoggerFactory.getLogger(MusicApiService.class);
    private final Gson gson = new Gson();

    @Value("${api.last.fm.key}")
    private String lastFmApiKey;

    @Autowired
    @Qualifier("lastFmRestClient")
    private RestClient lastFmRestClient;

    @Autowired
    @Qualifier("spotifyRestClient")
    private RestClient spotifyRestClient;

    @Autowired
    @Qualifier("itunesRestClient")
    private RestClient itunesRestClient;

    @Autowired
    private SpotifyTokenService spotifyTokenService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private ModelMapper modelMapper;

    // Spotify 검색 (track-id 조회용으로만 사용)
    public SpotifySearchResponse getTrackInfo(String term, String type, int limit) {
        try {
            String accessToken = spotifyTokenService.getAccessToken();

            if (accessToken == null) {
                logger.warn("[Spotify] accessToken 없음 - /spotify/login 필요");
                return null;
            }

            logger.info("[getTrackInfo] 검색어: {}, limit: {}", term, limit);

            String body = spotifyRestClient.get()
                    .uri(url -> url.path("/v1/search")
                            .queryParam("q", term)
                            .queryParam("type", "track")
                            .queryParam("market", "KR")
                            .queryParam("limit", limit)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            return gson.fromJson(body, SpotifySearchResponse.class);
        } catch (Exception e) {
            logger.error("[getTrackInfo] Spotify API 곡 정보 가져오기 실패 - 검색어: {}, error: {}", term, e.getMessage());
            return null;
        }
    }

    // iTunes 검색 전용
    private ItunesSearchResponse getItunesTrackInfo(String term, String attribute, int limit) {
        try {
            String body = itunesRestClient.get()
                    .uri(url -> url.path("/search")
                            .queryParam("term", term)
                            .queryParam("country", "KR")
                            .queryParam("media", "music")
                            .queryParam("entity", "song")
                            .queryParam("attribute", attribute)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(String.class);

            return gson.fromJson(body, ItunesSearchResponse.class);
        } catch (Exception e) {
            logger.error("[getItunesTrackInfo] iTunes 검색 실패 - 검색어: {}, error: {}", term, e.getMessage());
            return null;
        }
    }

    // iTunes previewUrl 조회
    private String getPreviewUrlFromItunes(String trackName, String artistName) {
        try {
            String term = trackName + " " + artistName;
            String body = itunesRestClient.get()
                    .uri(url -> url.path("/search")
                            .queryParam("term", term)
                            .queryParam("country", "KR")
                            .queryParam("media", "music")
                            .queryParam("entity", "song")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(String.class);

            ItunesSearchResponse response = gson.fromJson(body, ItunesSearchResponse.class);
            if (response != null && !response.getResults().isEmpty()) {
                return response.getResults().get(0).getPreviewUrl();
            }
        } catch (Exception e) {
            logger.error("[getPreviewUrlFromItunes] iTunes 미리듣기 조회 실패 - {}: {}", trackName, e.getMessage());
        }
        return null;
    }

    // ✅ iTunes 이미지 해상도 업그레이드 (Spotify API 호출 없음)
    private SongDTO upgradeImageResolution(SongDTO itunesSong) {
        if (itunesSong.getImgUrl() == null)
            return itunesSong;

        String highResImgUrl = itunesSong.getImgUrl()
                .replace("100x100bb", "600x600bb")
                .replace("60x60bb", "600x600bb");

        return SongDTO.builder()
                .id(itunesSong.getId())
                .trackName(itunesSong.getTrackName())
                .artistName(itunesSong.getArtistName())
                .previewUrl(itunesSong.getPreviewUrl())
                .imgUrl(highResImgUrl)
                .releaseDate(itunesSong.getReleaseDate())
                .durationMs(itunesSong.getDurationMs())
                .genreName(itunesSong.getGenreName())
                .build();
    }

    // last.fm 유사 곡
    public SimilarTracksResponse getSimilarTracks(String trackName, String artistName) {
        try {
            return lastFmRestClient.get()
                    .uri(url -> url.queryParam("method", "track.getSimilar")
                            .queryParam("track", trackName)
                            .queryParam("artist", artistName)
                            .queryParam("api_key", lastFmApiKey)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SimilarTracksResponse.class);
        } catch (Exception e) {
            logger.error("[getSimilarTracks] last.fm 유사 곡 실패 - {}, {}, error: {}", trackName, artistName,
                    e.getMessage());
            return null;
        }
    }

    // last.fm 유사 아티스트
    public SimilarArtistResponse getSimilarArtists(String artistName) {
        try {
            return lastFmRestClient.get()
                    .uri(url -> url.queryParam("method", "artist.getSimilar")
                            .queryParam("limit", "5")
                            .queryParam("artist", artistName)
                            .queryParam("api_key", lastFmApiKey)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SimilarArtistResponse.class);
        } catch (Exception e) {
            logger.error("[getSimilarArtists] last.fm 유사 아티스트 실패 - {}, error: {}", artistName, e.getMessage());
            return null;
        }
    }

    // 초기 곡 정보 DB 저장
    @Transactional
    public void saveInitialSongInfo() {
        String[] terms = {
                "뉴진스", "아일릿", "아이브", "르세라핌", "블랙핑크",
                "방탄소년단", "에스파", "데이식스", "악동뮤지션", "QWER",
                "트와이스", "레드벨벳", "엔믹스", "비와이",
                "볼빨간사춘기", "아이유", "태연", "헤이즈", "한로로",
                "Taylor Swift", "Bruno Mars", "Ariana Grande", "Justin Bieber", "Rihanna",
                "The Weeknd", "Billie Eilish", "Ed Sheeran", "Lady Gaga", "Coldplay",
                "Imagine Dragons", "Maroon 5", "Adele", "The Beatles", "Eminem"
        };

        for (String term : terms) {
            try {
                ItunesSearchResponse itunesResponse = getItunesTrackInfo(term, "artistTerm", 10);
                if (itunesResponse == null || itunesResponse.getResults().isEmpty())
                    continue;

                for (SongDTO itunesSong : itunesResponse.getResults()) {
                    try {
                        // ✅ Spotify 대신 iTunes 해상도 업그레이드
                        SongDTO finalSong = upgradeImageResolution(itunesSong);

                        Song song = modelMapper.map(finalSong, Song.class);
                        songRepository.findById(finalSong.getId())
                                .orElseGet(() -> songRepository.save(song));

                    } catch (Exception e) {
                        logger.error("[saveInitialSongInfo] 곡 처리 실패 - {}: {}",
                                itunesSong.getTrackName(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.error("[saveInitialSongInfo] 초기 곡 정보 DB 저장 실패 - term: {}, error: {}", term, e.getMessage());
            }
        }
    }

    // 추천 곡 리스트
    @Transactional
    public List<SongDTO> getRecommendSongList(String trackName, String artistName) {
        List<SongDTO> resultList = new ArrayList<>();
        try {
            List<SongDTO> similarTrackList = getSimilarTrackRecommendSongList(trackName, artistName);
            List<SongDTO> similarArtistList = getSimilarArtistRecommendSongList(trackName, artistName);

            if (!similarTrackList.isEmpty()) {
                resultList = similarTrackList;
            } else if (!similarArtistList.isEmpty()) {
                resultList = similarArtistList;
            }
            return resultList;
        } catch (Exception e) {
            logger.error("[getRecommendSongList] 추천 곡 리스트 실패 - error: {}", e.getMessage());
            return resultList;
        }
    }

    // 유사곡 기반 추천
    private List<SongDTO> getSimilarTrackRecommendSongList(String trackName, String artistName) {
        List<SongDTO> resultList = new ArrayList<>();
        try {
            SimilarTracksResponse similarTracksResponse = getSimilarTracks(trackName, artistName);
            if (similarTracksResponse == null)
                return resultList;

            List<TrackDTO> similarTrackList = similarTracksResponse.getSimilarTracks().getTrack();
            if (similarTrackList == null)
                return resultList;

            List<TrackDTO> filtered = similarTrackList.stream()
                    .filter(track -> track.getMatch() >= 0.7)
                    .collect(Collectors.toList());

            for (TrackDTO trackDTO : filtered) {
                try {
                    String itunesQuery = trackDTO.getName() + " " + trackDTO.getArtistDto().getName();
                    ItunesSearchResponse itunesResponse = getItunesTrackInfo(itunesQuery, "mixTerm", 1);

                    if (itunesResponse == null || itunesResponse.getResults().isEmpty())
                        continue;

                    SongDTO itunesSong = itunesResponse.getResults().get(0);

                    // ✅ Spotify 대신 iTunes 해상도 업그레이드
                    SongDTO songDto = upgradeImageResolution(itunesSong);

                    Song song = modelMapper.map(songDto, Song.class);
                    songRepository.findById(songDto.getId())
                            .orElseGet(() -> songRepository.save(song));

                    resultList.add(songDto);
                } catch (Exception e) {
                    logger.error("[getSimilarTrackRecommendSongList] 곡 처리 실패 - {}: {}",
                            trackDTO.getName(), e.getMessage());
                }
            }
            return resultList;
        } catch (Exception e) {
            logger.error("[getSimilarTrackRecommendSongList] 실패 - error: {}", e.getMessage());
            return resultList;
        }
    }

    // 유사 아티스트 기반 추천
    private List<SongDTO> getSimilarArtistRecommendSongList(String trackName, String artistName) {
        List<SongDTO> resultList = new ArrayList<>();
        try {
            SimilarArtistResponse similarArtistResponse = getSimilarArtists(artistName);
            if (similarArtistResponse == null)
                return resultList;

            List<ArtistDTO> similarArtistList = similarArtistResponse.getSimilarArtists().getArtist();
            if (similarArtistList == null)
                return resultList;

            List<ArtistDTO> filtered = similarArtistList.stream()
                    .filter(artist -> artist.getMatch() >= 0.7)
                    .collect(Collectors.toList());

            for (ArtistDTO artistDTO : filtered) {
                try {
                    ItunesSearchResponse itunesResponse = getItunesTrackInfo(artistDTO.getName(), "artistTerm", 3);

                    if (itunesResponse == null || itunesResponse.getResults().isEmpty())
                        continue;

                    for (SongDTO itunesSong : itunesResponse.getResults()) {
                        try {
                            // ✅ Spotify 대신 iTunes 해상도 업그레이드
                            SongDTO songDto = upgradeImageResolution(itunesSong);

                            Song song = modelMapper.map(songDto, Song.class);
                            songRepository.findById(songDto.getId())
                                    .orElseGet(() -> songRepository.save(song));

                            resultList.add(songDto);
                        } catch (Exception e) {
                            logger.error("[getSimilarArtistRecommendSongList] 곡 처리 실패 - {}: {}",
                                    itunesSong.getTrackName(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("[getSimilarArtistRecommendSongList] 아티스트 처리 실패 - {}: {}",
                            artistDTO.getName(), e.getMessage());
                }
            }
            return resultList;
        } catch (Exception e) {
            logger.error("[getSimilarArtistRecommendSongList] 실패 - error: {}", e.getMessage());
            return resultList;
        }
    }

    // ✅ 유저 검색 곡 리스트 — iTunes만 사용 (빠른 응답)
    public List<SongDTO> getSearchSongList(String query) {
        List<SongDTO> resultList = new ArrayList<>();

        ItunesSearchResponse itunesResponse = getItunesTrackInfo(query, "songTerm", 50);
        if (itunesResponse == null || itunesResponse.getResults().isEmpty()) {
            return resultList;
        }

        resultList = itunesResponse.getResults().stream()
                .map(this::upgradeImageResolution)
                .collect(Collectors.toList());

        // 한 번에 기존 ID 조회
        List<Long> ids = resultList.stream()
                .map(SongDTO::getId)
                .collect(Collectors.toList());
        List<Long> existingIds = songRepository.findAllById(ids)
                .stream()
                .map(Song::getId)
                .collect(Collectors.toList());

        // 없는 곡만 저장
        List<Song> newSongs = resultList.stream()
                .filter(dto -> !existingIds.contains(dto.getId()))
                .map(dto -> modelMapper.map(dto, Song.class))
                .collect(Collectors.toList());

        if (!newSongs.isEmpty()) {
            songRepository.saveAll(newSongs); // 한 번에 저장
        }

        return resultList;
    }
}
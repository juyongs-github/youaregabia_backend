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
import com.music.music.api.dto.SimilarArtistResponse;
import com.music.music.api.dto.SimilarTracksResponse;
import com.music.music.api.dto.TrackDTO;
import com.music.music.api.entity.SongDTO;
import com.music.music.api.repository.SongRepository;
import com.music.music.playlist.entity.Song;

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
    @Qualifier("itunesRestClient")
    private RestClient itunesRestClient;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private ModelMapper modelMapper;

    // 곡 정보 가져오기
    public ItunesSearchResponse getTrackInfo(String term, String attribute, int limit) {
        try {
            String body = itunesRestClient.get()
                    .uri((url) -> url.path("/search")
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
            logger.error("[getTrackInfo] itunes API 곡 정보 가져오기 실패 - 검색어: {}, error: {}", term, e.getMessage());
            return null;
        }
    }

    // 유사 곡 리스트 (곡 추천 메인)
    public SimilarTracksResponse getSimilarTracks(String trackName, String artistName) {
        try {
            return lastFmRestClient.get()
                    .uri((url) -> url.queryParam("method", "track.getSimilar")
                            .queryParam("track", trackName)
                            .queryParam("artist", artistName)
                            .queryParam("api_key", lastFmApiKey)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SimilarTracksResponse.class);
        } catch (Exception e) {
            logger.error("[getSimilarTracks] last.fm API 유사 곡 리스트 가져오기 실패 - 곡 이름: {}, 아티스트 이름: {}, error: {}",
                    trackName, artistName, e.getMessage());
            return null;
        }
    }

    // 유사 아티스트 리스트 (곡 추천 대안)
    public SimilarArtistResponse getSimilarArtists(String artistName) {
        try {
            return lastFmRestClient.get()
                    .uri((url) -> url.queryParam("method", "artist.getSimilar")
                            .queryParam("limit", "5")
                            .queryParam("artist", artistName)
                            .queryParam("api_key", lastFmApiKey)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(SimilarArtistResponse.class);
        } catch (Exception e) {
            logger.error("[getSimilarArtists] last.fm API 유사 아티스트 리스트 가져오기 실패 - 아티스트 이름: {}, error: {}", artistName,
                    e.getMessage());
            return null;
        }
    }

    // 초기 곡 정보 DB 저장 (곡 정보 확보)
    @Transactional
    public void saveInitialSongInfo() {
        String[] terms = {
                "뉴진스", "아일릿", "아이브", "르세라핌", "블랙핑크",
                "방탄소년단", "에스파", "데이식스", "악동뮤지션", "QWER",
                "블랙핑크", "트와이스", "레드벨벳", "엔믹스", "비와이",
                "볼빨간사춘기", "아이유", "태연", "헤이즈", "한로로",
                "Taylor Swift", "Bruno Mars", "Ariana Grande", "Justin Bieber", "Rihanna",
                "The Weeknd", "Billie Eilish", "Ed Sheeran", "Lady Gaga", "Coldplay",
                "Imagine Dragons", "Maroon 5", "Adele", "The Beatles", "Eminem"
        };

        for (String term : terms) {
            try {
                ItunesSearchResponse itunesSearchResponse = getTrackInfo(term, "artistTerm", 10);

                if (itunesSearchResponse == null) {
                    return;
                }

                List<SongDTO> songList = itunesSearchResponse.getResults();

                for (SongDTO songDto : songList) {
                    Song song = modelMapper.map(songDto, Song.class);
                    songRepository.findById(songDto.getId())
                            .orElseGet(() -> songRepository.save(song));
                }
            } catch (Exception e) {
                logger.error("[saveInitialSongInfo] 초기 곡 정보 DB 저장 실패 - error: {}", e.getMessage());
            }
        }
    }

    /**
     * 추천 곡 리스트 가져오기
     * 1. 유사 곡 기반 추천
     * 2. 유사 아티스트 기반 추천
     */
    @Transactional
    public List<SongDTO> getRecommendSongList(String trackName, String artistName) {
        List<SongDTO> resultList = new ArrayList<>();

        try {
            // 유사 곡 기반 곡 리스트
            List<SongDTO> similarTrackRecommendSongList = getSimilarTrackRecommendSongList(trackName, artistName);
            // 유사 아티스트 기반 곡 리스트
            List<SongDTO> similarArtistRecommendSongList = getSimilarArtistRecommendSongList(trackName, artistName);

            // 리스트 값 세팅
            if (!similarTrackRecommendSongList.isEmpty()) {
                resultList = similarTrackRecommendSongList;
            } else if (!similarArtistRecommendSongList.isEmpty()) {
                resultList = similarArtistRecommendSongList;
            }

            return resultList;
        } catch (Exception e) {
            logger.error("[getRecommendSongList] 추천 곡 리스트 가져오기 실패 - error: {}", e.getMessage());
            return resultList;
        }
    }

    // 유사곡 기반 추천 곡 리스트
    private List<SongDTO> getSimilarTrackRecommendSongList(String trackName, String artistName) {
        List<SongDTO> resultList = new ArrayList<>();

        try {
            SimilarTracksResponse similarTracksResponse = getSimilarTracks(trackName, artistName);

            if (similarTracksResponse != null) {
                List<TrackDTO> similarTrackList = similarTracksResponse.getSimilarTracks().getTrack();

                if (similarTrackList != null) {
                    // 유사도가 0.7 이상인 곡들로 필터링
                    List<TrackDTO> filterSimilarTrackList = similarTrackList.stream()
                            .filter((track) -> track.getMatch() >= 0.7)
                            .collect(Collectors.toList());

                    for (TrackDTO trackDTO : filterSimilarTrackList) {
                        String term = trackDTO.getName() + "+" + trackDTO.getArtistDto().getName(); // 곡 이름 + 아티스트 이름
                        ItunesSearchResponse itunesSearchResponse = getTrackInfo(term, "mixTerm", 1); // 해당 곡 정보 가져오기

                        // 가져오기 실패 시 skip
                        if (itunesSearchResponse == null) {
                            continue;
                        }

                        SongDTO songDto = itunesSearchResponse.getResults().get(0);

                        // DB에 추천 곡 정보 INSERT
                        Song song = modelMapper.map(songDto, Song.class);
                        songRepository.findById(songDto.getId())
                                .orElseGet(() -> songRepository.save(song));

                        resultList.add(songDto);
                    }
                }
            }

            return resultList;
        } catch (Exception e) {
            logger.error("[getSimilarTrackRecommendSongList] 유사곡 기반 추천 곡 리스트 가져오기 실패 - error: {}", e.getMessage());
            return resultList;
        }
    }

    // 유사 아티스트 기반 추천 곡 리스트
    private List<SongDTO> getSimilarArtistRecommendSongList(String trackName, String artistName) {
        List<SongDTO> resultList = new ArrayList<>();

        try {
            SimilarArtistResponse similarArtistResponse = getSimilarArtists(artistName);

            if (similarArtistResponse != null) {
                List<ArtistDTO> similarArtistList = similarArtistResponse.getSimilarArtists().getArtist();

                if (similarArtistList != null) {
                    // 유사도가 0.7 이상인 아티스트들로 필터링
                    List<ArtistDTO> filterSimilarArtistList = similarArtistList.stream()
                            .filter((artist) -> artist.getMatch() >= 0.7)
                            .collect(Collectors.toList());

                    for (ArtistDTO artistDTO : filterSimilarArtistList) {
                        String term = artistDTO.getName();
                        ItunesSearchResponse itunesSearchResponse = getTrackInfo(term, "artistTerm", 3); // 아티스트 당 3곡씩 가져오기

                        // 가져오기 실패 시 skip
                        if (itunesSearchResponse == null) {
                            continue;
                        }

                        List<SongDTO> songList = itunesSearchResponse.getResults();
                        for (SongDTO songDto : songList) {
                            // DB에 추천 곡 정보 INSERT
                            Song song = modelMapper.map(songDto, Song.class);
                            songRepository.findById(songDto.getId())
                                    .orElseGet(() -> songRepository.save(song));

                            resultList.add(songDto);
                        }
                    }
                }
            }

            return resultList;
        } catch (Exception e) {
            logger.error("[getSimilarArtistRecommendSongList] 유사 아티스트 기반 추천 곡 리스트 가져오기 실패 - error: {}", e.getMessage());
            return resultList;
        }
    }

    // 유저 검색 곡 리스트 가져오기
    public List<SongDTO> getSearchSongList(String query) {
        List<SongDTO> resultList = new ArrayList<>();
        ItunesSearchResponse itunesSearchResponse = getTrackInfo(query, "songTerm", 30);
    
        if(itunesSearchResponse != null) {
            List<SongDTO> songList = itunesSearchResponse.getResults();
            for(SongDTO songDto : songList) {
                // DB에 추천 곡 정보 INSERT
                Song song = modelMapper.map(songDto, Song.class);
                songRepository.findById(songDto.getId())
                    .orElseGet(() -> songRepository.save(song));

                resultList.add(songDto);
            }
        }
    
        return resultList;
    }
}
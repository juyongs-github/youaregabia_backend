package com.music.music.ranking;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.music.music.ranking.dto.ArtistRankingDto;
import com.music.music.ranking.dto.SongRankingDto;
import com.music.music.ranking.dto.UserRankingDto;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class RankingCache {
    private List<UserRankingDto> topLikeUsers = Collections.emptyList();
    private List<UserRankingDto> topPointUsers = Collections.emptyList();
    private List<SongRankingDto> topSharedSongs = Collections.emptyList();
    private List<ArtistRankingDto> topArtists = Collections.emptyList();
}

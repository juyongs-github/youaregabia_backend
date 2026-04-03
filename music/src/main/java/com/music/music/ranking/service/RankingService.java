package com.music.music.ranking.service;

import com.music.music.ranking.dto.ArtistRankingDto;
import com.music.music.ranking.dto.SongRankingDto;
import com.music.music.ranking.dto.UserRankingDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    @PersistenceContext
    private EntityManager em;

    private static final int TOP_N = 10;

    // 1. 좋아요 유저 랭킹 (게시글 likeCount + 댓글 likeCount 합산)
    // getTopLikeUsers()
    public List<UserRankingDto> getTopLikeUsers() {
        String jpql = """
            SELECT new com.music.music.ranking.dto.UserRankingDto(
                u.id,
                u.name,
                CAST(up.grade AS string),
                (
                    (SELECT COALESCE(SUM(b.likeCount), 0) FROM Board b WHERE b.user = u AND b.deleted = false)
                    +
                    (SELECT COALESCE(SUM(r.likeCount), 0) FROM Reply r WHERE r.user = u AND r.deleted = false)
                )
            )
            FROM users u
            JOIN UserPoint up ON up.user = u
            WHERE u.state = 1 
            AND u.role != com.music.music.user.entity.Role.ADMIN
            ORDER BY (
                (SELECT COALESCE(SUM(b.likeCount), 0) FROM Board b WHERE b.user = u AND b.deleted = false)
                +
                (SELECT COALESCE(SUM(r.likeCount), 0) FROM Reply r WHERE r.user = u AND r.deleted = false)
            ) DESC
            """;

        return em.createQuery(jpql, UserRankingDto.class)
                .setMaxResults(TOP_N)
                .getResultList();
    }

    // 2. getTopPointUsers() - 탈퇴 회원 필터링 추가
    public List<UserRankingDto> getTopPointUsers() {
        String jpql = """
            SELECT new com.music.music.ranking.dto.UserRankingDto(
                u.id,
                u.name,
                CAST(up.grade AS string),
                CAST(up.totalPoint AS long)
            )
            FROM UserPoint up
            JOIN up.user u
            WHERE up.totalPoint > 0
            AND u.state = 1 
            AND u.role != com.music.music.user.entity.Role.ADMIN
            ORDER BY up.totalPoint DESC
            """;

        return em.createQuery(jpql, UserRankingDto.class)
                .setMaxResults(TOP_N)
                .getResultList();
    }

    // 3. 일주일간 인기 곡 랭킹
    public List<SongRankingDto> getTopSharedSongs() {
    LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);

    String jpql = """
        SELECT new com.music.music.ranking.dto.SongRankingDto(
            s.id,
            s.trackName,
            s.artistName,
            s.imgUrl,
            COUNT(bs.id),
            s.previewUrl,
            s.genreName,
            s.durationMs,
            s.releaseDate
        )
        FROM BoardSong bs
        JOIN bs.song s
        JOIN bs.board b
        WHERE b.boardType = com.music.music.board.entity.BoardType.PLAYLIST_SHARE
          AND b.deleted = false
          AND b.createdAt >= :oneWeekAgo
        GROUP BY s.id, s.trackName, s.artistName, s.imgUrl, s.previewUrl, s.genreName, s.durationMs, s.releaseDate
        ORDER BY COUNT(bs.id) DESC
        """;

    return em.createQuery(jpql, SongRankingDto.class)
            .setParameter("oneWeekAgo", oneWeekAgo)
            .setMaxResults(TOP_N)
            .getResultList();
    }

    // 4. 일주일간 인기 가수 랭킹
    public List<ArtistRankingDto> getTopArtists() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);

        String jpql = """
            SELECT new com.music.music.ranking.dto.ArtistRankingDto(
                s.artistName,
                COUNT(bs.id)
            )
            FROM BoardSong bs
            JOIN bs.song s
            JOIN bs.board b
            WHERE b.boardType = com.music.music.board.entity.BoardType.PLAYLIST_SHARE
              AND b.deleted = false
              AND b.createdAt >= :oneWeekAgo
            GROUP BY s.artistName
            ORDER BY COUNT(bs.id) DESC
            """;

        return em.createQuery(jpql, ArtistRankingDto.class)
                .setParameter("oneWeekAgo", oneWeekAgo)
                .setMaxResults(TOP_N)
                .getResultList();
    }
}
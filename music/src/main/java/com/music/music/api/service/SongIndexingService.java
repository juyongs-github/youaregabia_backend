package com.music.music.api.service;

import com.music.music.playlist.entity.Song;
import com.music.music.playlist.repository.SongRepository;
import com.music.music.recommendation.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 검색 결과 반환과 분리해 백그라운드에서 신규 곡을 DB + 벡터 DB에 저장하는 서비스.
 */
@Service
public class SongIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(SongIndexingService.class);

    private final SongRepository songRepository;
    private final VectorSearchService vectorSearchService;

    public SongIndexingService(SongRepository songRepository, VectorSearchService vectorSearchService) {
        this.songRepository = songRepository;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * 신규 곡을 DB에 저장하고 벡터 인덱싱을 비동기로 수행한다.
     * 검색 결과 응답과 완전히 분리되어 백그라운드에서 실행된다.
     */
    @Async
    @Transactional
    public void saveAndIndex(List<Song> newSongs) {
        if (newSongs == null || newSongs.isEmpty()) return;
        try {
            List<Song> saved = songRepository.saveAll(newSongs);
            logger.info("[SongIndexingService] {}곡 DB 저장 완료, 벡터 인덱싱 시작", saved.size());
            vectorSearchService.indexSongs(saved);
            logger.info("[SongIndexingService] {}곡 벡터 인덱싱 완료", saved.size());
        } catch (Exception e) {
            logger.warn("[SongIndexingService] 백그라운드 저장/인덱싱 실패: {}", e.getMessage());
        }
    }
}

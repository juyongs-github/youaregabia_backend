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
     * 신규 곡을 DB에 즉시(동기) 저장하고, 벡터 인덱싱만 비동기로 수행한다.
     * DB 저장은 검색 응답 전에 완료되므로 곡 제안 시 타이밍 이슈가 없다.
     */
    @Transactional
    public List<Song> saveSync(List<Song> newSongs) {
        if (newSongs == null || newSongs.isEmpty()) return List.of();
        List<Song> saved = songRepository.saveAll(newSongs);
        logger.info("[SongIndexingService] {}곡 DB 저장 완료", saved.size());
        return saved;
    }

    /**
     * 벡터 인덱싱만 비동기로 수행한다. (DB 저장과 분리)
     */
    @Async
    public void indexAsync(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return;
        try {
            vectorSearchService.indexSongs(songs);
            logger.info("[SongIndexingService] {}곡 벡터 인덱싱 완료", songs.size());
        } catch (Exception e) {
            logger.warn("[SongIndexingService] 벡터 인덱싱 실패: {}", e.getMessage());
        }
    }

    /**
     * DB 저장(동기) + 벡터 인덱싱(비동기) 일괄 수행. 외부 호출용.
     */
    public void saveAndIndex(List<Song> newSongs) {
        List<Song> saved = saveSync(newSongs);
        if (!saved.isEmpty()) {
            indexAsync(saved);
        }
    }
}

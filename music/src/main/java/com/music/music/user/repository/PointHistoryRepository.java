package com.music.music.user.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.user.entity.PointHistory;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>{
    Page<PointHistory> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

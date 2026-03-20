package com.music.music.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.user.entity.PointHistory;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>{
    Page<PointHistory> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 적립만
    Page<PointHistory> findByUser_IdAndAmountGreaterThanOrderByCreatedAtDesc(Long userId, int amount, Pageable pageable);

    // 차감만
    Page<PointHistory> findByUser_IdAndAmountLessThanOrderByCreatedAtDesc(Long userId, int amount, Pageable pageable);
}

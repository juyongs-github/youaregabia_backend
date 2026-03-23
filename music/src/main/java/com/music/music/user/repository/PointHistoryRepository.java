package com.music.music.user.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.music.music.user.entity.PointHistory;
import com.music.music.user.entity.PointType;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long>{
    Page<PointHistory> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 적립만
    Page<PointHistory> findByUser_IdAndAmountGreaterThanOrderByCreatedAtDesc(Long userId, int amount, Pageable pageable);

    // 차감만
    Page<PointHistory> findByUser_IdAndAmountLessThanOrderByCreatedAtDesc(Long userId, int amount, Pageable pageable);

    // 관리자 지급/차감 로그
    List<PointHistory> findByPointTypeInOrderByCreatedAtDesc(List<PointType> types);

    // 회원탈퇴 시 삭제 (native query로 enum 매핑 오류 방지)
    @Modifying
    @Query(value = "DELETE FROM point_history WHERE user_id = :userId", nativeQuery = true)
    void deleteByUser_Id(@Param("userId") Long userId);
}

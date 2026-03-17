package com.music.music.goods.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.goods.entity.GoodsOrder;

public interface GoodsOrderRepository extends JpaRepository<GoodsOrder, Long> {
    List<GoodsOrder> findByUser_EmailOrderByCreatedAtDesc(String email);
    Optional<GoodsOrder> findByTossOrderId(String tossOrderId);
    List<GoodsOrder> findAllByOrderByCreatedAtDesc();
    List<GoodsOrder> findByUser_Id(Long userId);
}

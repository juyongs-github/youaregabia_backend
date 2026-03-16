package com.music.music.goods.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.goods.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}

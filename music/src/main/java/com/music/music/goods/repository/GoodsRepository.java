package com.music.music.goods.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.music.music.goods.entity.Goods;
import com.music.music.goods.entity.GoodsCategory;

public interface GoodsRepository extends JpaRepository<Goods, Long> {
    List<Goods> findByCategory(GoodsCategory category);
}

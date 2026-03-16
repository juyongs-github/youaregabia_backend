package com.music.music.goods.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.goods.dto.GoodsDto;
import com.music.music.goods.entity.Goods;
import com.music.music.goods.entity.GoodsCategory;
import com.music.music.goods.repository.GoodsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoodsService {

    private final GoodsRepository goodsRepository;

    public List<GoodsDto> getGoodsList(String category) {
        List<Goods> list = (category != null && !category.isBlank())
                ? goodsRepository.findByCategory(GoodsCategory.valueOf(category))
                : goodsRepository.findAll();
        return list.stream().map(GoodsDto::new).toList();
    }

    public GoodsDto getGoods(Long goodsId) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        return new GoodsDto(goods);
    }

    @Transactional
    public GoodsDto createGoods(GoodsDto.Request req, String imageUrl) {
        Goods goods = Goods.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .category(GoodsCategory.valueOf(req.getCategory()))
                .imageUrl(imageUrl)
                .build();
        return new GoodsDto(goodsRepository.save(goods));
    }

    @Transactional
    public void updateGoods(Long goodsId, GoodsDto.Request req, String imageUrl) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));
        goods.update(req.getName(), req.getDescription(), req.getPrice(), req.getStock(),
                GoodsCategory.valueOf(req.getCategory()), imageUrl);
    }

    @Transactional
    public void deleteGoods(Long goodsId) {
        goodsRepository.deleteById(goodsId);
    }
}

package com.music.music.goods.dto;

import java.time.LocalDateTime;

import com.music.music.goods.entity.Goods;
import com.music.music.goods.entity.GoodsCategory;

import lombok.Getter;

@Getter
public class GoodsDto {
    private Long goodsId;
    private String name;
    private String description;
    private int price;
    private int stock;
    private GoodsCategory category;
    private String imageUrl;
    private LocalDateTime createdAt;

    public GoodsDto(Goods g) {
        this.goodsId = g.getGoodsId();
        this.name = g.getName();
        this.description = g.getDescription();
        this.price = g.getPrice();
        this.stock = g.getStock();
        this.category = g.getCategory();
        this.imageUrl = g.getImageUrl();
        this.createdAt = g.getCreatedAt();
    }

    // 상품 등록/수정용 요청 DTO
    @Getter
    public static class Request {
        private String name;
        private String description;
        private int price;
        private int stock;
        private String category;
    }
}

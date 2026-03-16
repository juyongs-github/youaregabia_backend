package com.music.music.goods.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.music.music.goods.dto.GoodsDto;
import com.music.music.goods.service.GoodsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    @Value("${file.upload.path}")
    private String uploadPath;

    // 전체/카테고리별 상품 조회
    @GetMapping
    public ResponseEntity<List<GoodsDto>> getGoodsList(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(goodsService.getGoodsList(category));
    }

    // 상품 상세 조회
    @GetMapping("/{goodsId}")
    public ResponseEntity<GoodsDto> getGoods(@PathVariable Long goodsId) {
        return ResponseEntity.ok(goodsService.getGoods(goodsId));
    }

    // 상품 등록 (관리자)
    @PostMapping
    public ResponseEntity<GoodsDto> createGoods(
            @RequestPart GoodsDto.Request request,
            @RequestPart(required = false) MultipartFile image,
            @AuthenticationPrincipal String email) throws IOException {
        String imageUrl = saveImage(image);
        return ResponseEntity.ok(goodsService.createGoods(request, imageUrl));
    }

    // 상품 수정 (관리자)
    @PutMapping("/{goodsId}")
    public ResponseEntity<Void> updateGoods(
            @PathVariable Long goodsId,
            @RequestPart GoodsDto.Request request,
            @RequestPart(required = false) MultipartFile image,
            @AuthenticationPrincipal String email) throws IOException {
        String imageUrl = saveImage(image);
        goodsService.updateGoods(goodsId, request, imageUrl);
        return ResponseEntity.ok().build();
    }

    // 상품 삭제 (관리자)
    @DeleteMapping("/{goodsId}")
    public ResponseEntity<Void> deleteGoods(
            @PathVariable Long goodsId,
            @AuthenticationPrincipal String email) {
        goodsService.deleteGoods(goodsId);
        return ResponseEntity.ok().build();
    }

    private String saveImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return null;
        String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
        File dest = new File(uploadPath + "/" + fileName);
        dest.getParentFile().mkdirs();
        image.transferTo(dest);
        return "/uploads/" + fileName;
    }
}

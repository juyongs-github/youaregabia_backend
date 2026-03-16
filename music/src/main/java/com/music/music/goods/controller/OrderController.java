package com.music.music.goods.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.music.music.goods.dto.OrderDto;
import com.music.music.goods.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @RequestBody OrderDto.CreateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(orderService.createOrder(email, request));
    }

    // 내 주문 목록
    @GetMapping("/me")
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(orderService.getMyOrders(email));
    }
}

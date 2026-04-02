package com.music.music.goods.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.music.goods.dto.OrderDto;
import com.music.music.goods.entity.Goods;
import com.music.music.goods.entity.GoodsOrder;
import com.music.music.goods.entity.OrderItem;
import com.music.music.goods.entity.OrderStatus;
import com.music.music.goods.repository.GoodsOrderRepository;
import com.music.music.goods.repository.GoodsRepository;
import com.music.music.user.entity.User;
import com.music.music.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final GoodsOrderRepository goodsOrderRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderDto createOrder(String email, OrderDto.CreateRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        GoodsOrder order = GoodsOrder.builder()
                .user(user)
                .tossOrderId(UUID.randomUUID().toString())
                .totalAmount(req.getTotalAmount())
                .receiverName(req.getReceiverName())
                .receiverPhone(req.getReceiverPhone())
                .deliveryAddress(req.getDeliveryAddress())
                .build();

        GoodsOrder savedOrder = goodsOrderRepository.save(order);

        for (OrderDto.CreateRequest.OrderItemRequest itemReq : req.getItems()) {
            Goods goods = goodsRepository.findById(itemReq.getGoodsId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품: " + itemReq.getGoodsId()));

            if (goods.getStock() < itemReq.getQuantity()) {
                throw new IllegalStateException("재고가 부족합니다: " + goods.getName());
            }

            goods.updateStock(-itemReq.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .goods(goods)
                    .quantity(itemReq.getQuantity())
                    .price(goods.getPrice())
                    .build();

            savedOrder.getOrderItems().add(orderItem);
        }

        return new OrderDto(savedOrder);
    }

    public List<OrderDto> getMyOrders(String email) {
        return goodsOrderRepository.findByUser_EmailOrderByCreatedAtDesc(email)
                .stream().map(OrderDto::new).toList();
    }

    public List<OrderDto> getAllOrders() {
        return goodsOrderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(OrderDto::new).toList();
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        GoodsOrder order = goodsOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        order.updateStatus(status);
    }

    @Transactional
    public void updateTracking(Long orderId, String carrierId, String trackingNumber) {
        GoodsOrder order = goodsOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        order.updateTracking(carrierId, trackingNumber);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        GoodsOrder order = goodsOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        goodsOrderRepository.delete(order);
    }
}

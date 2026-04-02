package com.music.music.goods.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.music.music.goods.entity.GoodsOrder;
import com.music.music.goods.entity.OrderStatus;

import lombok.Getter;

@Getter
public class OrderDto {
    private Long orderId;
    private String tossOrderId;
    private int totalAmount;
    private OrderStatus status;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private LocalDateTime createdAt;
    private List<OrderItemDto> items;
    private String carrierId;
    private String trackingNumber;
    private String userEmail;

    public OrderDto(GoodsOrder o) {
        this.orderId = o.getOrderId();
        this.tossOrderId = o.getTossOrderId();
        this.totalAmount = o.getTotalAmount();
        this.status = o.getStatus();
        this.receiverName = o.getReceiverName();
        this.receiverPhone = o.getReceiverPhone();
        this.deliveryAddress = o.getDeliveryAddress();
        this.createdAt = o.getCreatedAt();
        this.items = o.getOrderItems().stream().map(OrderItemDto::new).toList();
        this.carrierId = o.getCarrierId();
        this.trackingNumber = o.getTrackingNumber();
        this.userEmail = o.getUser() != null ? o.getUser().getEmail() : null;
    }

    @Getter
    public static class OrderItemDto {
        private Long goodsId;
        private String goodsName;
        private int price;
        private int quantity;

        public OrderItemDto(com.music.music.goods.entity.OrderItem item) {
            this.goodsId = item.getGoods().getGoodsId();
            this.goodsName = item.getGoods().getName();
            this.price = item.getPrice();
            this.quantity = item.getQuantity();
        }
    }

    // 주문 생성 요청 DTO
    @Getter
    public static class CreateRequest {
        private List<OrderItemRequest> items;
        private String receiverName;
        private String receiverPhone;
        private String deliveryAddress;
        private int totalAmount;

        @Getter
        public static class OrderItemRequest {
            private Long goodsId;
            private int quantity;
        }
    }
}

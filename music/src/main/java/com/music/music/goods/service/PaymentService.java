package com.music.music.goods.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.music.music.goods.entity.GoodsOrder;
import com.music.music.goods.entity.OrderStatus;
import com.music.music.goods.repository.GoodsOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${toss.secret-key}")
    private String secretKey;

    private final GoodsOrderRepository goodsOrderRepository;

    @Transactional
    public void confirmPayment(String paymentKey, String tossOrderId, int amount) {
        GoodsOrder order = goodsOrderRepository.findByTossOrderId(tossOrderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (order.getTotalAmount() != amount) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        callTossConfirmApi(paymentKey, tossOrderId, amount);

        order.updateStatus(OrderStatus.PAID);
    }

    private void callTossConfirmApi(String paymentKey, String orderId, int amount) {
        String credentials = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", paymentKey);
        body.put("orderId", orderId);
        body.put("amount", amount);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            new RestTemplate().postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    request,
                    String.class);
        } catch (HttpClientErrorException e) {
            throw new IllegalStateException("결제 승인 실패: " + e.getResponseBodyAsString());
        }
    }
}

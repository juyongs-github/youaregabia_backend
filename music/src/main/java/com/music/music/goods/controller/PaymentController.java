package com.music.music.goods.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.music.music.goods.service.PaymentService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @RequestBody ConfirmRequest req,
            @AuthenticationPrincipal String email) {
        paymentService.confirmPayment(req.getPaymentKey(), req.getOrderId(), req.getAmount());
        return ResponseEntity.ok().build();
    }

    @Getter
    public static class ConfirmRequest {
        private String paymentKey;
        private String orderId;
        private int amount;
    }
}

package com.ordersystem.api.controller;

import com.ordersystem.api.dto.request.CreatePaymentRequest;
import com.ordersystem.api.dto.response.OrderPaymentSummaryResponse;
import com.ordersystem.api.dto.response.PaymentResponse;
import com.ordersystem.application.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PaymentResponse> approvePayment(@PathVariable Long id) {
        PaymentResponse response = paymentService.approvePayment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PaymentResponse> rejectPayment(@PathVariable Long id) {
        PaymentResponse response = paymentService.rejectPayment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<PaymentResponse> markPaymentAsFailed(@PathVariable Long id) {
        PaymentResponse response = paymentService.markPaymentAsFailed(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<PaymentResponse> response = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}/summary")
    public ResponseEntity<OrderPaymentSummaryResponse> getOrderPaymentSummary(@PathVariable Long orderId) {
        OrderPaymentSummaryResponse response = paymentService.getOrderPaymentSummary(orderId);
        return ResponseEntity.ok(response);
    }
}



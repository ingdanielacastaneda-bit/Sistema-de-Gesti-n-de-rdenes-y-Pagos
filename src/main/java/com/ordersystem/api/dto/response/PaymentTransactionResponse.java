package com.ordersystem.api.dto.response;

import com.ordersystem.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {
    private Long id;
    private PaymentStatus previousStatus;
    private PaymentStatus newStatus;
    private LocalDateTime timestamp;
    private String notes;
}


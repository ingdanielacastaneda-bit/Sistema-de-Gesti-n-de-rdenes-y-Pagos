package com.ordersystem.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que agrupa la información de una orden con todos sus pagos y transacciones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentSummaryResponse {
    private OrderResponse order;
    private List<PaymentResponse> payments;
}



package com.ordersystem.api.dto.response;

import com.ordersystem.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStateHistoryResponse {
    private Long id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private LocalDateTime timestamp;
    private String notes;
}



package com.ordersystem.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long customerId;
    
    @NotEmpty(message = "La orden debe tener al menos un ítem")
    @Valid
    private List<CreateOrderItemRequest> items;
}



package com.ordersystem.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de dominio emitido cuando una orden es marcada como pagada
 */
@Getter
public class OrderPaidEvent extends ApplicationEvent {

    private final Long orderId;
    private final Long customerId;
    private final BigDecimal totalAmount;
    private final Instant occurredAt;

    public OrderPaidEvent(Object source, Long orderId, Long customerId, BigDecimal totalAmount) {
        super(source);
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.occurredAt = Instant.now();
    }
}


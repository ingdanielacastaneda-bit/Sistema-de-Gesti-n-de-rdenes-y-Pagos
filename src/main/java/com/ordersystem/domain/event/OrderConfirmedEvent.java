package com.ordersystem.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando una orden es confirmada
 */
@Getter
public class OrderConfirmedEvent extends ApplicationEvent {

    private final Long orderId;
    private final Long customerId;
    private final Instant occurredAt;

    public OrderConfirmedEvent(Object source, Long orderId, Long customerId) {
        super(source);
        this.orderId = orderId;
        this.customerId = customerId;
        this.occurredAt = Instant.now();
    }
}


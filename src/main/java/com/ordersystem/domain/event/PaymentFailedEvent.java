package com.ordersystem.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de dominio emitido cuando un pago falla
 */
@Getter
public class PaymentFailedEvent extends ApplicationEvent {

    private final Long paymentId;
    private final Long orderId;
    private final BigDecimal amount;
    private final Instant occurredAt;

    public PaymentFailedEvent(Object source, Long paymentId, Long orderId, BigDecimal amount) {
        super(source);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.occurredAt = Instant.now();
    }
}


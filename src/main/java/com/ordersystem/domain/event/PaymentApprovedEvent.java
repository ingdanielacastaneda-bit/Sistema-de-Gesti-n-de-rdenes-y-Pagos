package com.ordersystem.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de dominio emitido cuando un pago es aprobado
 */
@Getter
public class PaymentApprovedEvent extends ApplicationEvent {

    private final Long paymentId;
    private final Long orderId;
    private final BigDecimal amount;
    private final Instant occurredAt;

    public PaymentApprovedEvent(Object source, Long paymentId, Long orderId, BigDecimal amount) {
        super(source);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.occurredAt = Instant.now();
    }
}


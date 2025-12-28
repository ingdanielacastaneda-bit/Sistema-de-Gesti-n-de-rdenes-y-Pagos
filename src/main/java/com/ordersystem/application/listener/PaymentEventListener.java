package com.ordersystem.application.listener;

import com.ordersystem.domain.event.PaymentApprovedEvent;
import com.ordersystem.domain.event.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener de eventos de dominio relacionados con pagos
 */
@Slf4j
@Component
public class PaymentEventListener {

    @EventListener
    @Async
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        log.info("Evento recibido: Pago {} aprobado (monto: {}) para orden {}", 
            event.getPaymentId(), event.getAmount(), event.getOrderId());
        
        // Aquí se puede agregar lógica adicional como:
        // - Notificar al proveedor de pagos
        // - Actualizar saldos
        // - Registrar en sistema contable
    }

    @EventListener
    @Async
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("Evento recibido: Pago {} fallido (monto: {}) para orden {}", 
            event.getPaymentId(), event.getAmount(), event.getOrderId());
        
        // Aquí se puede agregar lógica adicional como:
        // - Notificar al cliente
        // - Registrar intento fallido
        // - Activar flujo de reintento
    }
}



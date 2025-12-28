package com.ordersystem.application.listener;

import com.ordersystem.domain.event.OrderConfirmedEvent;
import com.ordersystem.domain.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener de eventos de dominio relacionados con órdenes
 * 
 * Estos listeners permiten ejecutar lógica adicional cuando ocurren eventos importantes
 * sin acoplar la lógica al flujo principal de negocio
 */
@Slf4j
@Component
public class OrderEventListener {

    @EventListener
    @Async
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Evento recibido: Orden {} confirmada para cliente {}", 
            event.getOrderId(), event.getCustomerId());
        
        // Aquí se puede agregar lógica adicional como:
        // - Enviar notificaciones
        // - Actualizar índices de búsqueda
        // - Integrar con sistemas externos
        // - Generar documentos
    }

    @EventListener
    @Async
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("Evento recibido: Orden {} pagada (monto: {}) para cliente {}", 
            event.getOrderId(), event.getTotalAmount(), event.getCustomerId());
        
        // Aquí se puede agregar lógica adicional como:
        // - Notificar al área de logística
        // - Generar factura
        // - Actualizar inventario
        // - Enviar confirmación al cliente
    }
}



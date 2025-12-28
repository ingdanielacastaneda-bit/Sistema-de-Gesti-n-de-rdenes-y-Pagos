package com.ordersystem.domain.model;

import com.ordersystem.domain.enums.OrderStatus;
import com.ordersystem.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para las reglas de negocio de la entidad Order
 */
@DisplayName("Tests de reglas de negocio - Order")
class OrderTest {

    private Customer customer;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
            .id(1L)
            .name("Cliente Test")
            .email("cliente@test.com")
            .build();

        order = Order.builder()
            .customer(customer)
            .status(OrderStatus.CREATED)
            .totalAmount(BigDecimal.valueOf(100.00))
            .build();
    }

    @Test
    @DisplayName("No se puede cancelar una orden pagada")
    void shouldNotCancelPaidOrder() {
        // Given
        order.setStatus(OrderStatus.PAID);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> order.cancel(),
            "Debería lanzar excepción al intentar cancelar una orden pagada");
    }

    @Test
    @DisplayName("No se puede cancelar una orden enviada")
    void shouldNotCancelShippedOrder() {
        // Given
        order.setStatus(OrderStatus.SHIPPED);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> order.cancel(),
            "Debería lanzar excepción al intentar cancelar una orden enviada");
    }

    @Test
    @DisplayName("Se puede cancelar una orden en estado CREATED")
    void shouldCancelCreatedOrder() {
        // Given
        order.setStatus(OrderStatus.CREATED);

        // When
        order.cancel();

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(1, order.getStateHistory().size());
        assertEquals(OrderStatus.CREATED, order.getStateHistory().get(0).getPreviousStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStateHistory().get(0).getNewStatus());
    }

    @Test
    @DisplayName("Se puede cancelar una orden en estado CONFIRMED")
    void shouldCancelConfirmedOrder() {
        // Given
        order.setStatus(OrderStatus.CONFIRMED);

        // When
        order.cancel();

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(1, order.getStateHistory().size());
    }

    @Test
    @DisplayName("No se puede confirmar una orden que no está en CREATED")
    void shouldNotConfirmNonCreatedOrder() {
        // Given
        order.setStatus(OrderStatus.PAID);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> order.confirm(),
            "Debería lanzar excepción al intentar confirmar una orden desde un estado inválido (PAID)");
    }

    @Test
    @DisplayName("Se puede confirmar una orden en estado CREATED")
    void shouldConfirmCreatedOrder() {
        // Given
        order.setStatus(OrderStatus.CREATED);

        // When
        order.confirm();

        // Then
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(1, order.getStateHistory().size());
        assertEquals(OrderStatus.CREATED, order.getStateHistory().get(0).getPreviousStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStateHistory().get(0).getNewStatus());
    }

    @Test
    @DisplayName("No se puede marcar como pagada una orden que no está en CONFIRMED")
    void shouldNotMarkAsPaidNonConfirmedOrder() {
        // Given
        order.setStatus(OrderStatus.CREATED);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> order.markAsPaid(),
            "Debería lanzar excepción al intentar marcar como pagada una orden no confirmada");
    }

    @Test
    @DisplayName("Se puede marcar como pagada una orden en estado CONFIRMED")
    void shouldMarkAsPaidConfirmedOrder() {
        // Given
        order.setStatus(OrderStatus.CONFIRMED);

        // When
        order.markAsPaid();

        // Then
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(1, order.getStateHistory().size());
    }

    @Test
    @DisplayName("No se puede enviar una orden que no está en PAID")
    void shouldNotShipNonPaidOrder() {
        // Given
        order.setStatus(OrderStatus.CONFIRMED);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> order.markAsShipped(),
            "Debería lanzar excepción al intentar enviar una orden no pagada");
    }

    @Test
    @DisplayName("Se puede enviar una orden en estado PAID")
    void shouldShipPaidOrder() {
        // Given
        order.setStatus(OrderStatus.PAID);

        // When
        order.markAsShipped();

        // Then
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals(1, order.getStateHistory().size());
    }

    @Test
    @DisplayName("Confirmar una orden ya confirmada es idempotente")
    void shouldBeIdempotentWhenConfirmingAlreadyConfirmedOrder() {
        // Given
        order.setStatus(OrderStatus.CONFIRMED);
        int historySizeBefore = order.getStateHistory().size();

        // When
        order.confirm(); // No debería lanzar excepción ni agregar historial

        // Then
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(historySizeBefore, order.getStateHistory().size(), 
            "No debería agregar nuevo registro al historial si ya está confirmada");
    }

    @Test
    @DisplayName("Cancelar una orden ya cancelada es idempotente")
    void shouldBeIdempotentWhenCancellingAlreadyCancelledOrder() {
        // Given
        order.setStatus(OrderStatus.CANCELLED);
        int historySizeBefore = order.getStateHistory().size();

        // When
        order.cancel(); // No debería lanzar excepción ni agregar historial

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(historySizeBefore, order.getStateHistory().size());
    }

    @Test
    @DisplayName("El historial registra todas las transiciones de estado")
    void shouldRecordAllStateTransitions() {
        // Given
        order.setStatus(OrderStatus.CREATED);

        // When
        order.confirm();
        order.markAsPaid();
        order.markAsShipped();

        // Then
        assertEquals(3, order.getStateHistory().size());
        assertEquals(OrderStatus.CREATED, order.getStateHistory().get(0).getPreviousStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStateHistory().get(0).getNewStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStateHistory().get(1).getPreviousStatus());
        assertEquals(OrderStatus.PAID, order.getStateHistory().get(1).getNewStatus());
        assertEquals(OrderStatus.PAID, order.getStateHistory().get(2).getPreviousStatus());
        assertEquals(OrderStatus.SHIPPED, order.getStateHistory().get(2).getNewStatus());
    }
}



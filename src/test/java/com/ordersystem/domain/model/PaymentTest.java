package com.ordersystem.domain.model;

import com.ordersystem.domain.enums.PaymentStatus;
import com.ordersystem.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para las reglas de negocio de la entidad Payment
 */
@DisplayName("Tests de reglas de negocio - Payment")
class PaymentTest {

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
            .id(1L)
            .name("Cliente Test")
            .email("cliente@test.com")
            .build();

        order = Order.builder()
            .id(1L)
            .customer(customer)
            .status(com.ordersystem.domain.enums.OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.valueOf(100.00))
            .build();

        payment = Payment.builder()
            .order(order)
            .amount(BigDecimal.valueOf(100.00))
            .status(PaymentStatus.PENDING)
            .build();
    }

    @Test
    @DisplayName("No se puede aprobar un pago que no está en PENDING")
    void shouldNotApproveNonPendingPayment() {
        // Given
        payment.setStatus(PaymentStatus.REJECTED);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> payment.approve(),
            "Debería lanzar excepción al intentar aprobar un pago desde un estado inválido (REJECTED)");
    }

    @Test
    @DisplayName("Se puede aprobar un pago en estado PENDING")
    void shouldApprovePendingPayment() {
        // Given
        payment.setStatus(PaymentStatus.PENDING);

        // When
        payment.approve();

        // Then
        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals(1, payment.getTransactions().size());
        assertEquals(PaymentStatus.PENDING, payment.getTransactions().get(0).getPreviousStatus());
        assertEquals(PaymentStatus.APPROVED, payment.getTransactions().get(0).getNewStatus());
    }

    @Test
    @DisplayName("No se puede rechazar un pago que no está en PENDING")
    void shouldNotRejectNonPendingPayment() {
        // Given
        payment.setStatus(PaymentStatus.APPROVED);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> payment.reject(),
            "Debería lanzar excepción al intentar rechazar un pago que no está en PENDING");
    }

    @Test
    @DisplayName("Se puede rechazar un pago en estado PENDING")
    void shouldRejectPendingPayment() {
        // Given
        payment.setStatus(PaymentStatus.PENDING);

        // When
        payment.reject();

        // Then
        assertEquals(PaymentStatus.REJECTED, payment.getStatus());
        assertEquals(1, payment.getTransactions().size());
    }

    @Test
    @DisplayName("No se puede marcar como fallido un pago que no está en PENDING")
    void shouldNotMarkAsFailedNonPendingPayment() {
        // Given
        payment.setStatus(PaymentStatus.APPROVED);

        // When/Then
        assertThrows(InvalidStateTransitionException.class, () -> payment.markAsFailed(),
            "Debería lanzar excepción al intentar marcar como fallido un pago que no está en PENDING");
    }

    @Test
    @DisplayName("Se puede marcar como fallido un pago en estado PENDING")
    void shouldMarkAsFailedPendingPayment() {
        // Given
        payment.setStatus(PaymentStatus.PENDING);

        // When
        payment.markAsFailed();

        // Then
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(1, payment.getTransactions().size());
        assertEquals(PaymentStatus.PENDING, payment.getTransactions().get(0).getPreviousStatus());
        assertEquals(PaymentStatus.FAILED, payment.getTransactions().get(0).getNewStatus());
    }

    @Test
    @DisplayName("Aprobar un pago ya aprobado es idempotente")
    void shouldBeIdempotentWhenApprovingAlreadyApprovedPayment() {
        // Given
        payment.setStatus(PaymentStatus.APPROVED);
        int transactionsSizeBefore = payment.getTransactions().size();

        // When
        payment.approve(); // No debería lanzar excepción ni agregar transacción

        // Then
        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals(transactionsSizeBefore, payment.getTransactions().size(),
            "No debería agregar nueva transacción si ya está aprobado");
    }

    @Test
    @DisplayName("Rechazar un pago ya rechazado es idempotente")
    void shouldBeIdempotentWhenRejectingAlreadyRejectedPayment() {
        // Given
        payment.setStatus(PaymentStatus.REJECTED);
        int transactionsSizeBefore = payment.getTransactions().size();

        // When
        payment.reject(); // No debería lanzar excepción ni agregar transacción

        // Then
        assertEquals(PaymentStatus.REJECTED, payment.getStatus());
        assertEquals(transactionsSizeBefore, payment.getTransactions().size());
    }

    @Test
    @DisplayName("El historial registra todas las transiciones de estado del pago")
    void shouldRecordAllPaymentStateTransitions() {
        // Given
        payment.setStatus(PaymentStatus.PENDING);
        payment.recordInitialState(); // Simular estado inicial

        // When
        payment.approve();

        // Then
        assertEquals(2, payment.getTransactions().size());
        assertEquals(PaymentStatus.PENDING, payment.getTransactions().get(0).getPreviousStatus());
        assertEquals(PaymentStatus.PENDING, payment.getTransactions().get(0).getNewStatus());
        assertEquals(PaymentStatus.PENDING, payment.getTransactions().get(1).getPreviousStatus());
        assertEquals(PaymentStatus.APPROVED, payment.getTransactions().get(1).getNewStatus());
    }
}



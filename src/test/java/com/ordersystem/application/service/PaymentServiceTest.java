package com.ordersystem.application.service;

import com.ordersystem.api.dto.request.CreatePaymentRequest;
import com.ordersystem.domain.enums.OrderStatus;
import com.ordersystem.domain.enums.PaymentStatus;
import com.ordersystem.domain.exception.BusinessRuleException;
import com.ordersystem.domain.exception.EntityNotFoundException;
import com.ordersystem.domain.model.Customer;
import com.ordersystem.domain.model.Order;
import com.ordersystem.domain.model.Payment;
import com.ordersystem.domain.repository.OrderRepository;
import com.ordersystem.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para las reglas de negocio del servicio de pagos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de reglas de negocio - PaymentService")
@SuppressWarnings("null")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Customer customer;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
            .id(1L)
            .name("Cliente Test")
            .email("cliente@test.com")
            .build();

        order = Order.builder()
            .id(1L)
            .customer(customer)
            .status(OrderStatus.CONFIRMED)
            .totalAmount(BigDecimal.valueOf(100.00))
            .items(new ArrayList<>())
            .build();

        payment = Payment.builder()
            .id(1L)
            .order(order)
            .amount(BigDecimal.valueOf(100.00))
            .status(PaymentStatus.PENDING)
            .transactions(new ArrayList<>())
            .build();
    }

    @Test
    @DisplayName("No se puede crear un pago para una orden no confirmada")
    void shouldNotCreatePaymentForNonConfirmedOrder() {
        // Given
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        CreatePaymentRequest request = new CreatePaymentRequest(1L, BigDecimal.valueOf(100.00));

        // When/Then
        assertThrows(BusinessRuleException.class, () -> paymentService.createPayment(request),
            "Debería lanzar excepción al intentar crear pago para orden no confirmada");
    }

    @Test
    @DisplayName("No se puede crear un pago si el monto excede el pendiente")
    void shouldNotCreatePaymentWhenAmountExceedsPending() {
        // Given
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        
        // Simular un pago aprobado previo de 50.00
        Payment existingPayment = Payment.builder()
            .id(2L)
            .order(order)
            .amount(BigDecimal.valueOf(50.00))
            .status(PaymentStatus.APPROVED)
            .build();
        when(paymentRepository.findByOrderId(1L)).thenReturn(java.util.List.of(existingPayment));
        
        CreatePaymentRequest request = new CreatePaymentRequest(1L, BigDecimal.valueOf(60.00)); // Excede pendiente

        // When/Then
        assertThrows(BusinessRuleException.class, () -> paymentService.createPayment(request),
            "Debería lanzar excepción cuando el monto excede el pendiente");
    }

    @Test
    @DisplayName("Un pago fallido no cambia el estado de la orden")
    void shouldNotChangeOrderStatusWhenPaymentFails() {
        // Given
        when(paymentRepository.findByIdWithTransactions(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        paymentService.markPaymentAsFailed(1L);

        // Then
        verify(orderService, never()).markOrderAsPaidInternal(any());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus(), 
            "El estado de la orden no debería cambiar cuando un pago falla");
    }

    @Test
    @DisplayName("Se puede crear un pago para una orden confirmada con monto válido")
    void shouldCreatePaymentForConfirmedOrderWithValidAmount() {
        // Given
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(new ArrayList<>());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        CreatePaymentRequest request = new CreatePaymentRequest(1L, BigDecimal.valueOf(100.00));

        // When
        var result = paymentService.createPayment(request);

        // Then
        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher, never()).publishEvent(any()); // No debería emitir evento en creación
    }

    @Test
    @DisplayName("Al aprobar un pago, si el total pagado alcanza el total de la orden, la orden se marca como pagada")
    void shouldMarkOrderAsPaidWhenPaymentApprovedAndTotalReached() {
        // Given
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByIdWithTransactions(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByOrderId(1L)).thenReturn(java.util.List.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        paymentService.approvePayment(1L);

        // Then
        verify(orderService).markOrderAsPaidInternal(1L);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Lanzar excepción cuando el pago no existe")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        when(paymentRepository.findByIdWithTransactions(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(EntityNotFoundException.class, () -> paymentService.approvePayment(999L),
            "Debería lanzar excepción cuando el pago no existe");
    }
}



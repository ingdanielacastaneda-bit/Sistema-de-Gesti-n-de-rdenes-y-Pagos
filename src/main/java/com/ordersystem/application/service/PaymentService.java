package com.ordersystem.application.service;

import com.ordersystem.api.dto.request.CreatePaymentRequest;
import com.ordersystem.api.dto.response.OrderItemResponse;
import com.ordersystem.api.dto.response.OrderPaymentSummaryResponse;
import com.ordersystem.api.dto.response.OrderResponse;
import com.ordersystem.api.dto.response.PaymentResponse;
import com.ordersystem.api.dto.response.PaymentTransactionResponse;
import com.ordersystem.domain.enums.OrderStatus;
import com.ordersystem.domain.enums.PaymentStatus;
import com.ordersystem.domain.event.PaymentApprovedEvent;
import com.ordersystem.domain.event.PaymentFailedEvent;
import com.ordersystem.domain.exception.BusinessRuleException;
import com.ordersystem.domain.exception.EntityNotFoundException;
import com.ordersystem.domain.model.Order;
import com.ordersystem.domain.model.Payment;
import com.ordersystem.domain.repository.OrderRepository;
import com.ordersystem.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // Validar que la orden existe
        Order order = orderRepository.findByIdWithItems(request.getOrderId())
            .orElseThrow(() -> new EntityNotFoundException("Orden", request.getOrderId()));

        // Validar que la orden esté en estado CONFIRMED
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessRuleException(
                "PAYMENT_ONLY_ON_CONFIRMED_ORDER",
                String.format("Solo se puede crear un pago para una orden en estado CONFIRMED. Estado actual: %s", 
                    order.getStatus())
            );
        }

        // Validar que el monto del pago no exceda el monto pendiente
        BigDecimal totalPaid = paymentRepository.findByOrderId(request.getOrderId()).stream()
            .filter(p -> p.getStatus() == PaymentStatus.APPROVED)
            .map(Payment::getAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        BigDecimal pendingAmount = order.getTotalAmount().subtract(totalPaid);
        if (request.getAmount().compareTo(pendingAmount) > 0) {
            throw new BusinessRuleException(
                "PAYMENT_AMOUNT_EXCEEDS_PENDING",
                String.format("El monto del pago (%.2f) excede el monto pendiente (%.2f)", 
                    request.getAmount(), pendingAmount)
            );
        }

        // Crear el pago
        Payment payment = Payment.builder()
            .order(order)
            .amount(request.getAmount())
            .status(PaymentStatus.PENDING)
            .build();

        // Registrar el estado inicial en el historial
        payment.recordInitialState();

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    public PaymentResponse approvePayment(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
            .orElseThrow(() -> new EntityNotFoundException("Pago", paymentId));

        // Guardar el estado anterior para verificar si cambió
        PaymentStatus previousStatus = payment.getStatus();

        // La validación del estado y cambio se realiza en el método approve() de la entidad
        // El método es idempotente y registra la transacción automáticamente
        payment.approve();

        Payment saved = paymentRepository.save(payment);

        // Emitir evento solo si el estado cambió
        if (previousStatus != saved.getStatus()) {
            Order orderForEvent = saved.getOrder();
            Long orderIdForEvent = orderForEvent != null ? orderForEvent.getId() : null;
            
            eventPublisher.publishEvent(new PaymentApprovedEvent(
                this, 
                saved.getId(), 
                orderIdForEvent, 
                saved.getAmount()
            ));
            log.info("Pago {} aprobado - Evento emitido", saved.getId());

            // Verificar si la orden debe marcarse como PAID
            // Recalcular el total pagado incluyendo el pago recién aprobado
            Order order = saved.getOrder();
            if (order != null && order.getId() != null) {
                BigDecimal totalPaid = paymentRepository.findByOrderId(order.getId()).stream()
                    .filter(p -> p.getStatus() == PaymentStatus.APPROVED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Si el total pagado es mayor o igual al total de la orden, marcar como PAID
                if (order.getTotalAmount() != null && totalPaid.compareTo(order.getTotalAmount()) >= 0) {
                    orderService.markOrderAsPaidInternal(order.getId());
                }
            }
        }

        return mapToResponse(saved);
    }

    public PaymentResponse rejectPayment(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
            .orElseThrow(() -> new EntityNotFoundException("Pago", paymentId));

        // La validación del estado se realiza en el método reject() de la entidad
        // El método es idempotente y registra la transacción automáticamente
        payment.reject();

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    public PaymentResponse markPaymentAsFailed(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
            .orElseThrow(() -> new EntityNotFoundException("Pago", paymentId));

        // Guardar el estado anterior para emitir evento
        PaymentStatus previousStatus = payment.getStatus();

        // La validación del estado se realiza en el método markAsFailed() de la entidad
        // El método es idempotente y registra la transacción automáticamente
        payment.markAsFailed();

        Payment saved = paymentRepository.save(payment);

        // Emitir evento solo si el estado cambió
        // Un pago fallido NO cambia el estado de la orden (regla de negocio)
        if (previousStatus != saved.getStatus()) {
            Order orderForEvent = saved.getOrder();
            Long orderIdForEvent = orderForEvent != null ? orderForEvent.getId() : null;
            
            eventPublisher.publishEvent(new PaymentFailedEvent(
                this, 
                saved.getId(), 
                orderIdForEvent, 
                saved.getAmount()
            ));
            log.info("Pago {} marcado como fallido - Evento emitido", saved.getId());
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
            .orElseThrow(() -> new EntityNotFoundException("Pago", paymentId));

        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderIdWithTransactions(orderId);
        return payments.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderPaymentSummaryResponse getOrderPaymentSummary(Long orderId) {
        Order order = orderRepository.findByIdWithItemsAndCustomer(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Orden", orderId));

        List<PaymentResponse> payments = getPaymentsByOrderId(orderId);

        OrderResponse orderResponse = OrderResponse.builder()
            .id(order.getId())
            .customerId(order.getCustomer().getId())
            .customerName(order.getCustomer().getName())
            .customerEmail(order.getCustomer().getEmail())
            .items(order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                    .id(item.getId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .build())
                .collect(Collectors.toList()))
            .totalAmount(order.getTotalAmount())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .build();

        return OrderPaymentSummaryResponse.builder()
            .order(orderResponse)
            .payments(payments)
            .build();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        // Manejar caso donde transactions pueda ser null
        List<PaymentTransactionResponse> transactions = payment.getTransactions() != null ?
            payment.getTransactions().stream()
                .map(tx -> PaymentTransactionResponse.builder()
                    .id(tx.getId())
                    .previousStatus(tx.getPreviousStatus())
                    .newStatus(tx.getNewStatus())
                    .timestamp(tx.getTimestamp())
                    .notes(tx.getNotes())
                    .build())
                .collect(Collectors.toList()) : new ArrayList<>();

        // Manejar caso donde order pueda ser null
        Long orderId = payment.getOrder() != null ? payment.getOrder().getId() : null;

        return PaymentResponse.builder()
            .id(payment.getId())
            .orderId(orderId)
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .createdAt(payment.getCreatedAt())
            .transactions(transactions)
            .build();
    }
}


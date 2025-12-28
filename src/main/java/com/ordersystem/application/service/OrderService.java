package com.ordersystem.application.service;

import com.ordersystem.api.dto.request.CreateOrderRequest;
import com.ordersystem.api.dto.response.OrderItemResponse;
import com.ordersystem.api.dto.response.OrderResponse;
import com.ordersystem.api.dto.response.OrderStateHistoryResponse;
import com.ordersystem.domain.enums.OrderStatus;
import com.ordersystem.domain.event.OrderConfirmedEvent;
import com.ordersystem.domain.event.OrderPaidEvent;
import com.ordersystem.domain.exception.EntityNotFoundException;
import com.ordersystem.domain.model.Customer;
import com.ordersystem.domain.model.Order;
import com.ordersystem.domain.model.OrderItem;
import com.ordersystem.domain.repository.CustomerRepository;
import com.ordersystem.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    @SuppressWarnings("null") // JPA save() siempre retorna un objeto no-null
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Validar que el cliente existe
        Long customerId = Objects.requireNonNull(request.getCustomerId(), "El ID del cliente no puede ser null");
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Cliente", customerId));

        // Crear la orden
        Order order = Order.builder()
            .customer(customer)
            .status(OrderStatus.CREATED)
            .build();

        // Agregar ítems y calcular total
        request.getItems().forEach(itemRequest -> {
            OrderItem item = OrderItem.builder()
                .productName(itemRequest.getProductName())
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .build();
            
            order.addItem(item);
        });

        Order saved = orderRepository.save(order);
        // JPA save siempre retorna un objeto no-null
        return mapToResponse(saved);
    }

    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepository.findByIdWithItems(Objects.requireNonNull(orderId, "El ID de la orden no puede ser null"))
            .orElseThrow(() -> new EntityNotFoundException("Orden", orderId));

        // Guardar el estado anterior para verificar si cambió
        OrderStatus previousStatus = order.getStatus();

        // La validación del estado y cambio se realiza en el método confirm() de la entidad
        // El método es idempotente: si ya está confirmada, no hace nada
        order.confirm();

        Order saved = orderRepository.save(order);
        // JPA save siempre retorna un objeto no-null

        // Emitir evento solo si el estado cambió
        if (previousStatus != saved.getStatus()) {
            eventPublisher.publishEvent(new OrderConfirmedEvent(this, saved.getId(), saved.getCustomer().getId()));
            log.info("Orden {} confirmada - Evento emitido", saved.getId());
        }

        return mapToResponse(saved);
    }

    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Orden", orderId));

        // La validación del estado se realiza en el método cancel() de la entidad
        // El método es idempotente: si ya está cancelada, no hace nada
        order.cancel();

        Order saved = orderRepository.save(order);
        // JPA save siempre retorna un objeto no-null
        return mapToResponse(saved);
    }

    public OrderResponse markOrderAsShipped(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Orden", orderId));

        // La validación del estado se realiza en el método markAsShipped() de la entidad
        // El método es idempotente: si ya está enviada, no hace nada
        order.markAsShipped();

        Order saved = orderRepository.save(order);
        // JPA save siempre retorna un objeto no-null
        return mapToResponse(saved);
    }

    /**
     * Marca una orden como pagada desde el servicio de pagos
     * Este método se llama internamente cuando un pago es aprobado
     */
    void markOrderAsPaidInternal(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Orden", orderId));

        OrderStatus previousStatus = order.getStatus();

        // La validación del estado se realiza en el método markAsPaid() de la entidad
        order.markAsPaid();

        Order saved = orderRepository.save(order);
        // JPA save siempre retorna un objeto no-null

        // Emitir evento solo si el estado cambió
        if (previousStatus != saved.getStatus()) {
            eventPublisher.publishEvent(new OrderPaidEvent(
                this, 
                saved.getId(), 
                saved.getCustomer().getId(), 
                saved.getTotalAmount()
            ));
            log.info("Orden {} marcada como pagada - Evento emitido", saved.getId());
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithItemsAndCustomer(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Orden", orderId));

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        // Cargar todas las relaciones necesarias dentro de la transacción
        return orders.stream()
            .map(order -> {
                // Forzar la carga de relaciones lazy dentro de la transacción
                order.getItems().size(); // Inicializar la colección
                order.getStateHistory().size(); // Inicializar la colección
                if (order.getCustomer() != null) {
                    order.getCustomer().getName(); // Inicializar la relación
                }
                return mapToResponse(order);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        // Cargar todas las relaciones necesarias dentro de la transacción
        return orders.stream()
            .map(order -> {
                // Forzar la carga de relaciones lazy dentro de la transacción
                order.getItems().size(); // Inicializar la colección
                order.getStateHistory().size(); // Inicializar la colección
                if (order.getCustomer() != null) {
                    order.getCustomer().getName(); // Inicializar la relación
                }
                return mapToResponse(order);
            })
            .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        // Manejar caso donde items pueda ser null (aunque no debería)
        List<OrderItemResponse> items = order.getItems() != null ? order.getItems().stream()
            .map(item -> OrderItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build())
            .collect(Collectors.toList()) : new ArrayList<>();

        // Manejar caso donde stateHistory pueda ser null
        List<OrderStateHistoryResponse> stateHistory = order.getStateHistory() != null ? 
            order.getStateHistory().stream()
                .map(history -> OrderStateHistoryResponse.builder()
                    .id(history.getId())
                    .previousStatus(history.getPreviousStatus())
                    .newStatus(history.getNewStatus())
                    .timestamp(history.getTimestamp())
                    .notes(history.getNotes())
                    .build())
                .collect(Collectors.toList()) : new ArrayList<>();

        // Manejar caso donde customer pueda ser null
        Long customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        String customerEmail = order.getCustomer() != null ? order.getCustomer().getEmail() : null;

        return OrderResponse.builder()
            .id(order.getId())
            .customerId(customerId)
            .customerName(customerName)
            .customerEmail(customerEmail)
            .items(items)
            .totalAmount(order.getTotalAmount())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .stateHistory(stateHistory)
            .build();
    }
}


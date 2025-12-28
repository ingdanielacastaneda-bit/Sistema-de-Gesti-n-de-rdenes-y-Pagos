package com.ordersystem.domain.model;

import com.ordersystem.domain.enums.OrderStatus;
import com.ordersystem.domain.exception.InvalidStateTransitionException;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad principal que representa una orden de compra
 * 
 * Contiene toda la lógica de negocio relacionada con estados y transiciones
 * Todas las transiciones de estado se registran en el historial
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto total debe ser mayor a 0")
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderStateHistory> stateHistory = new ArrayList<>();

    /**
     * Cambia el estado de la orden y registra el cambio en el historial
     * Este método es privado y solo debe ser llamado desde los métodos públicos de transición
     */
    private void changeStatus(OrderStatus newStatus, String notes) {
        OrderStatus previousStatus = this.status;
        if (previousStatus == newStatus) {
            return; // Idempotencia: si ya está en el estado deseado, no hacer nada
        }
        
        this.status = newStatus;
        
        // Registrar en el historial
        OrderStateHistory history = OrderStateHistory.builder()
            .order(this)
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .notes(notes)
            .build();
        
        this.stateHistory.add(history);
    }

    /**
     * Regla de negocio: Confirma una orden
     * Solo se puede confirmar una orden en estado CREATED
     * 
     * @throws InvalidStateTransitionException si la orden no está en estado CREATED
     */
    public void confirm() {
        if (this.status == OrderStatus.CONFIRMED) {
            return; // Idempotencia
        }
        
        if (this.status != OrderStatus.CREATED) {
            throw new InvalidStateTransitionException(
                "Order",
                this.status.name(),
                OrderStatus.CONFIRMED.name(),
                "Solo se puede confirmar una orden desde el estado CREATED"
            );
        }
        
        changeStatus(OrderStatus.CONFIRMED, "Orden confirmada");
    }

    /**
     * Regla de negocio: Marca una orden como pagada
     * Solo se puede marcar como pagada una orden en estado CONFIRMED
     * 
     * @throws InvalidStateTransitionException si la orden no está en estado CONFIRMED
     */
    public void markAsPaid() {
        if (this.status == OrderStatus.PAID) {
            return; // Idempotencia
        }
        
        if (this.status != OrderStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(
                "Order",
                this.status.name(),
                OrderStatus.PAID.name(),
                "Solo se puede marcar como pagada una orden desde el estado CONFIRMED"
            );
        }
        
        changeStatus(OrderStatus.PAID, "Orden marcada como pagada");
    }

    /**
     * Regla de negocio: Cancela una orden
     * Solo se puede cancelar una orden en estado CREATED o CONFIRMED
     * No se puede cancelar una orden que ya fue pagada
     * 
     * @throws InvalidStateTransitionException si la orden no puede ser cancelada
     */
    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            return; // Idempotencia
        }
        
        if (this.status == OrderStatus.PAID || this.status == OrderStatus.SHIPPED) {
            throw new InvalidStateTransitionException(
                "Order",
                this.status.name(),
                OrderStatus.CANCELLED.name(),
                "No se puede cancelar una orden que ya fue pagada o enviada"
            );
        }
        
        if (this.status != OrderStatus.CREATED && this.status != OrderStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(
                "Order",
                this.status.name(),
                OrderStatus.CANCELLED.name(),
                "Solo se puede cancelar una orden desde los estados CREATED o CONFIRMED"
            );
        }
        
        changeStatus(OrderStatus.CANCELLED, "Orden cancelada");
    }

    /**
     * Regla de negocio: Marca una orden como enviada
     * Solo se puede marcar como enviada una orden en estado PAID
     * 
     * @throws InvalidStateTransitionException si la orden no está en estado PAID
     */
    public void markAsShipped() {
        if (this.status == OrderStatus.SHIPPED) {
            return; // Idempotencia
        }
        
        if (this.status != OrderStatus.PAID) {
            throw new InvalidStateTransitionException(
                "Order",
                this.status.name(),
                OrderStatus.SHIPPED.name(),
                "Solo se puede marcar como enviada una orden desde el estado PAID"
            );
        }
        
        changeStatus(OrderStatus.SHIPPED, "Orden enviada");
    }

    /**
     * Calcula el monto total de la orden basándose en los ítems
     */
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Agrega un ítem a la orden y recalcula el total
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
        calculateTotalAmount();
    }

    /**
     * Agrega un registro al historial de estados (usado internamente)
     */
    public void addStateHistory(OrderStateHistory history) {
        history.setOrder(this);
        this.stateHistory.add(history);
    }
}


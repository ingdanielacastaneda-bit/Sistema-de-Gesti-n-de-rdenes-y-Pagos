package com.ordersystem.domain.model;

import com.ordersystem.domain.enums.PaymentStatus;
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
 * Entidad que representa un pago asociado a una orden
 * 
 * Contiene la lógica de negocio relacionada con estados de pago
 * Todas las transiciones de estado se registran en el historial de transacciones
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La orden es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "El monto del pago es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto del pago debe ser mayor a 0")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentTransaction> transactions = new ArrayList<>();

    /**
     * Cambia el estado del pago y registra el cambio en el historial
     * Este método es privado y solo debe ser llamado desde los métodos públicos de transición
     */
    private void changeStatus(PaymentStatus newStatus, String notes) {
        PaymentStatus previousStatus = this.status;
        if (previousStatus == newStatus) {
            return; // Idempotencia: si ya está en el estado deseado, no hacer nada
        }
        
        this.status = newStatus;
        
        // Registrar en el historial
        PaymentTransaction transaction = PaymentTransaction.builder()
            .payment(this)
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .notes(notes)
            .build();
        
        this.transactions.add(transaction);
    }

    /**
     * Regla de negocio: Aprueba un pago
     * Solo se puede aprobar un pago en estado PENDING
     * 
     * @throws InvalidStateTransitionException si el pago no está en estado PENDING
     */
    public void approve() {
        if (this.status == PaymentStatus.APPROVED) {
            return; // Idempotencia
        }
        
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Payment",
                this.status.name(),
                PaymentStatus.APPROVED.name(),
                "Solo se puede aprobar un pago desde el estado PENDING"
            );
        }
        
        changeStatus(PaymentStatus.APPROVED, "Pago aprobado");
    }

    /**
     * Regla de negocio: Rechaza un pago
     * Solo se puede rechazar un pago en estado PENDING
     * 
     * @throws InvalidStateTransitionException si el pago no está en estado PENDING
     */
    public void reject() {
        if (this.status == PaymentStatus.REJECTED) {
            return; // Idempotencia
        }
        
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Payment",
                this.status.name(),
                PaymentStatus.REJECTED.name(),
                "Solo se puede rechazar un pago desde el estado PENDING"
            );
        }
        
        changeStatus(PaymentStatus.REJECTED, "Pago rechazado");
    }

    /**
     * Regla de negocio: Marca un pago como fallido
     * Solo se puede marcar como fallido un pago en estado PENDING
     * 
     * @throws InvalidStateTransitionException si el pago no está en estado PENDING
     */
    public void markAsFailed() {
        if (this.status == PaymentStatus.FAILED) {
            return; // Idempotencia
        }
        
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Payment",
                this.status.name(),
                PaymentStatus.FAILED.name(),
                "Solo se puede marcar como fallido un pago desde el estado PENDING"
            );
        }
        
        changeStatus(PaymentStatus.FAILED, "Pago fallido");
    }

    /**
     * Registra el estado inicial del pago en el historial
     * Debe ser llamado una vez al crear el pago
     */
    public void recordInitialState() {
        PaymentTransaction initialTransaction = PaymentTransaction.builder()
            .payment(this)
            .previousStatus(this.status) // Estado inicial
            .newStatus(this.status)      // Estado inicial
            .notes("Pago creado")
            .build();
        this.transactions.add(initialTransaction);
    }

    /**
     * Agrega una transacción al historial de pagos (usado internamente)
     */
    public void addTransaction(PaymentTransaction transaction) {
        transaction.setPayment(this);
        this.transactions.add(transaction);
    }
}


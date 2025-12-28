package com.ordersystem.domain.model;

import com.ordersystem.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una transacción dentro del historial de un pago
 * 
 * Registra todos los cambios de estado de un pago para auditoría
 * Nunca se elimina ni se sobrescribe - mantiene un registro completo
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_transaction_payment_id", columnList = "payment_id"),
    @Index(name = "idx_payment_transaction_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El pago es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus newStatus;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(length = 500)
    private String notes;
}


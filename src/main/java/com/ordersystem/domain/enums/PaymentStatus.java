package com.ordersystem.domain.enums;

/**
 * Estados válidos de un pago
 * 
 * Transiciones permitidas:
 * - PENDING -> APPROVED, REJECTED, FAILED
 * - APPROVED -> (estado final)
 * - REJECTED -> (estado final)
 * - FAILED -> (estado final)
 */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FAILED
}



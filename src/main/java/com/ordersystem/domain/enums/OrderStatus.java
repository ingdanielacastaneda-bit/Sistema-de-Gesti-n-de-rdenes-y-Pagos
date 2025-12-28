package com.ordersystem.domain.enums;

/**
 * Estados válidos de una orden
 * 
 * Transiciones permitidas:
 * - CREATED -> CONFIRMED, CANCELLED
 * - CONFIRMED -> PAID, CANCELLED
 * - PAID -> SHIPPED
 * - CANCELLED -> (estado final, no transiciones)
 * - SHIPPED -> (estado final, no transiciones)
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    PAID,
    SHIPPED,
    CANCELLED
}



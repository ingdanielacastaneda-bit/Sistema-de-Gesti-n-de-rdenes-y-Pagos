package com.ordersystem.domain.exception;

/**
 * Excepción base para todas las excepciones de dominio
 * 
 * Las excepciones de dominio representan violaciones de reglas de negocio
 * y deben ser manejadas de manera específica en la capa de aplicación
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}



package com.ordersystem.domain.exception;

/**
 * Excepción lanzada cuando se intenta realizar una transición de estado inválida
 */
public class InvalidStateTransitionException extends DomainException {

    private final String currentState;
    private final String targetState;
    private final String entityType;

    public InvalidStateTransitionException(String entityType, String currentState, String targetState, String reason) {
        super(String.format("No se puede transicionar %s de estado %s a %s. %s", 
            entityType, currentState, targetState, reason));
        this.entityType = entityType;
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getTargetState() {
        return targetState;
    }

    public String getEntityType() {
        return entityType;
    }
}



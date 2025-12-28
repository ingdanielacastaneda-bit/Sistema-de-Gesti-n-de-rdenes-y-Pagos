package com.ordersystem.domain.exception;

/**
 * Excepción lanzada cuando se viola una regla de negocio específica
 */
public class BusinessRuleException extends DomainException {

    private final String ruleName;

    public BusinessRuleException(String ruleName, String message) {
        super(message);
        this.ruleName = ruleName;
    }

    public String getRuleName() {
        return ruleName;
    }
}



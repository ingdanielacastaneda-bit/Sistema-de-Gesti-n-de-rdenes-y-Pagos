package com.ordersystem.domain.exception;

/**
 * Excepción lanzada cuando una entidad no se encuentra en el repositorio
 */
public class EntityNotFoundException extends DomainException {

    private final String entityType;
    private final Object identifier;

    public EntityNotFoundException(String entityType, Object identifier) {
        super(String.format("%s no encontrado con identificador: %s", entityType, identifier));
        this.entityType = entityType;
        this.identifier = identifier;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getIdentifier() {
        return identifier;
    }
}



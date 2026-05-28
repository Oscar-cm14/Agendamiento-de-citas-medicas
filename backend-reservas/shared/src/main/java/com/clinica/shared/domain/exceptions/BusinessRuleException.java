package com.clinica.shared.domain.exceptions;

/**
 * Excepción para violaciones de reglas de negocio.
 * El GlobalExceptionHandler la convierte en HTTP 409 Conflict
 * con el mensaje legible para el frontend.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
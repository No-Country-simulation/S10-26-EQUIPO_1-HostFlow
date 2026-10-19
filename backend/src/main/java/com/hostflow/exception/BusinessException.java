package com.hostflow.exception;

import lombok.Getter;

/**
 * Excepción personalizada para errores de lógica de negocio en HostFlow.
 *
 * <p>Se lanza cuando ocurre una violación de las reglas de negocio,
 * como un correo electrónico ya registrado o términos no aceptados.
 * Esta excepción es manejada por el {@link GlobalExceptionHandler}
 * para retornar una respuesta HTTP 400 (Bad Request).</p>
 *
 * @author HostFlow Team
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Código de error asociado a la excepción.
     */
    private final String errorCode;

    /**
     * Constructor con mensaje de error.
     *
     * @param message el mensaje descriptivo del error
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    /**
     * Constructor con mensaje y código de error.
     *
     * @param message   el mensaje descriptivo del error
     * @param errorCode el código identificador del error
     */
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
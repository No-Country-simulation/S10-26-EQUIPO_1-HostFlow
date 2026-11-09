package com.hostflow.exception;

import lombok.Getter;

/**
 * Excepción personalizada para errores de lógica de negocio en HostFlow.
 *
 * <p>Se lanza cuando ocurre una violación de las reglas de negocio,
 * como un correo electrónico ya registrado, validaciones de datos fallidas
 * o términos no aceptados. Esta excepción es interceptada y manejada
 * automáticamente por el {@link com.hostflow.config.GlobalExceptionHandler}
 * para retornar una respuesta HTTP 400 (Bad Request) con un cuerpo de error estandarizado.</p>
 *
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * if (user.isRegistered()) {
 *     throw new BusinessException("El correo electrónico ya está registrado", "EMAIL_ALREADY_EXISTS");
 * }
 * }</pre>
 *
 * @author HostFlow Team
 * @version 1.1.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Código de error estandarizado para facilitar el manejo, la identificación
     * y el logging del tipo de error en el lado del cliente (frontend) y en los logs del servidor.
     */
    private final String errorCode;

    /**
     * Construye una nueva excepción de negocio con el mensaje de error especificado.
     *
     * @param message el mensaje descriptivo del error de negocio
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    /**
     * Construye una nueva excepción de negocio con el mensaje y el código de error especificados.
     *
     * @param message   el mensaje descriptivo del error de negocio
     * @param errorCode el código identificador único del error
     */
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Construye una nueva excepción de negocio con el mensaje, la causa raíz y el código de error.
     *
     * <p>Este constructor es útil para el <strong>encadenamiento de excepciones</strong> (exception chaining),
     * permitiendo preservar el stack trace original de la excepción de bajo nivel
     * que provocó este error de negocio.</p>
     *
     * @param message   el mensaje descriptivo del error de negocio
     * @param cause     la causa raíz (throwable) que provocó esta excepción
     * @param errorCode el código identificador único del error
     */
    public BusinessException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
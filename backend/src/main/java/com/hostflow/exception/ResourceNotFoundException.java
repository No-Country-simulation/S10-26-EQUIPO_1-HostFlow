package com.hostflow.exception;

import lombok.Getter;

/**
 * Excepción personalizada para recursos no encontrados en HostFlow.
 *
 * <p>Se lanza cuando se intenta acceder a un recurso que no existe
 * en la base de datos, como un dueño con un ID o email específico.
 * Esta excepción es manejada por el {@link GlobalExceptionHandler}
 * para retornar una respuesta HTTP 404 (Not Found).</p>
 *
 * @author HostFlow Team
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Código de error asociado a la excepción.
     */
    private final String errorCode;

    /**
     * Constructor con mensaje de error.
     *
     * @param message el mensaje descriptivo del recurso no encontrado
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = "RESOURCE_NOT_FOUND";
    }
}
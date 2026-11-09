package com.hostflow.exception;

import lombok.Getter;

/**
 * Excepción personalizada para indicar que un recurso solicitado no fue encontrado.
 *
 * <p>Se lanza cuando se intenta acceder, actualizar o eliminar un recurso que no existe
 * en la base de datos (por ejemplo, un propietario, alojamiento, huésped o reserva con
 * un ID o email inexistente). Esta excepción es interceptada automáticamente por el
 * {@link com.hostflow.config.GlobalExceptionHandler} (o tu clase de manejo de errores)
 * para retornar una respuesta HTTP 404 (Not Found) con un cuerpo de error estandarizado.</p>
 *
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>{@code
 * Owner owner = ownerRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + id));
 * }</pre>
 *
 * @author HostFlow Team
 * @version 1.1.0
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Código de error estandarizado para facilitar el manejo, la identificación
     * y el logging del tipo de error en el lado del cliente (frontend) y en los logs del servidor.
     */
    private final String errorCode;

    /**
     * Construye una nueva excepción de recurso no encontrado con el mensaje especificado.
     *
     * @param message el mensaje descriptivo que detalla qué recurso no fue encontrado
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = "RESOURCE_NOT_FOUND";
    }

    /**
     * Construye una nueva excepción de recurso no encontrado con el mensaje y la causa especificados.
     *
     * <p>Este constructor es útil para el <strong>encadenamiento de excepciones</strong> (exception chaining),
     * permitiendo preservar el stack trace original de la excepción de bajo nivel (ej. SQLException)
     * que provocó este error de negocio.</p>
     *
     * @param message el mensaje descriptivo que detalla qué recurso no fue encontrado
     * @param cause   la causa raíz (throwable) que provocó esta excepción
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RESOURCE_NOT_FOUND";
    }
}
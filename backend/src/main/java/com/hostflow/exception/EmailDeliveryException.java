package com.hostflow.exception;

/**
 * Se lanza cuando falla el envío de un correo que forma parte de una operación
 * de negocio que exige confirmación de entrega al proveedor (modo estricto).
 *
 * <p>Usado principalmente en la creación y renovación de enlaces de reserva:
 * si el proveedor rechaza el mensaje o no hay proveedor configurado, la
 * operación debe abortarse para no dejar una reserva sin seguimiento.</p>
 */
public class EmailDeliveryException extends BusinessException {

    public EmailDeliveryException(String message) {
        super(message, "EMAIL_DELIVERY_FAILED");
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause, "EMAIL_DELIVERY_FAILED");
    }
}

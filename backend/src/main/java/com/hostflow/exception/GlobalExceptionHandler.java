package com.hostflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la API de HostFlow.
 *
 * <p>Captura todas las excepciones lanzadas en los controladores
 * y las transforma en respuestas HTTP estandarizadas con códigos
 * de estado apropiados. Esto garantiza una experiencia consistente
 * para los consumidores de la API.</p>
 *
 * <p>Tipos de excepciones manejadas:</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} → 404 Not Found</li>
 *   <li>{@link BusinessException} → 400 Bad Request</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request (validaciones)</li>
 *   <li>{@link BadCredentialsException} → 401 Unauthorized</li>
 *   <li>{@link UsernameNotFoundException} → 401 Unauthorized</li>
 *   <li>Excepciones genéricas → 500 Internal Server Error</li>
 * </ul>
 *
 * @author HostFlow Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja las excepciones de recurso no encontrado.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 404 con detalles del error
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getErrorCode());
    }

    /**
     * Maneja las excepciones de lógica de negocio.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 400 con detalles del error
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode());
    }

    /**
     * Maneja las excepciones de validación de datos (Bean Validation).
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 400 con los campos que fallaron la validación
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        // Recopilar todos los errores de validación por campo
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Error de validación");
        errorResponse.put("message", "Los datos enviados no son válidos");
        errorResponse.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Maneja las excepciones de credenciales incorrectas.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 401 con mensaje de error
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,
                "Credenciales incorrectas. Verifique su correo y contraseña.",
                "INVALID_CREDENTIALS");
    }

    /**
     * Maneja las excepciones de usuario no encontrado durante autenticación.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 401 con mensaje de error
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,
                "No se encontró una cuenta con esas credenciales.",
                "USER_NOT_FOUND");
    }

    /**
     * Maneja todas las excepciones no capturadas específicamente.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 500 con mensaje genérico
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado en el servidor",
                "INTERNAL_ERROR");
    }

    /**
     * Construye una respuesta de error estandarizada.
     *
     * @param status    el código de estado HTTP
     * @param message   el mensaje descriptivo del error
     * @param errorCode el código identificador del error
     * @return la respuesta HTTP con el cuerpo de error
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, String errorCode) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("errorCode", errorCode);

        return ResponseEntity.status(status).body(errorResponse);
    }
}
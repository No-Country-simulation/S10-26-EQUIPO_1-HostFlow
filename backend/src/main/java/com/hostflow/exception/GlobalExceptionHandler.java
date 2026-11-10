package com.hostflow.exception;

import com.hostflow.exception.CloudinaryDeleteException;
import com.hostflow.exception.CloudinaryUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la API de HostFlow.
 *
 * <p>Captura todas las excepciones no controladas lanzadas en los controladores
 * y las transforma en respuestas HTTP estandarizadas con códigos de estado
 * apropiados. Esto garantiza una experiencia de consumo de API consistente y predecible.</p>
 *
 * <p><strong>Categorías de excepciones manejadas:</strong></p>
 * <ul>
 *   <li><strong>Negocio:</strong> {@link ResourceNotFoundException}, {@link BusinessException}</li>
 *   <li><strong>Validación:</strong> {@link MethodArgumentNotValidException}</li>
 *   <li><strong>Seguridad:</strong> {@link BadCredentialsException}, {@link UsernameNotFoundException}, {@link AccessDeniedException}</li>
 *   <li><strong>Archivos:</strong> Excepciones de multipart, tamaño excedido y errores de Cloudinary.</li>
 *   <li><strong>General:</strong> {@link Exception} (fallback para errores no previstos).</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // 1. EXCEPCIONES DE NEGOCIO Y RECURSOS
    // =========================================================================

    /**
     * Maneja las excepciones de recurso no encontrado.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 404 Not Found con detalles del error
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getErrorCode());
    }

    /**
     * Maneja las excepciones de lógica de negocio personalizada.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 400 Bad Request con detalles del error
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrorCode());
    }

    // =========================================================================
    // 2. EXCEPCIONES DE VALIDACIÓN DE DATOS
    // =========================================================================

    /**
     * Maneja las excepciones de validación de datos (Bean Validation / Jakarta Validation).
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 400 Bad Request detallando los campos que fallaron la validación
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
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

    // =========================================================================
    // 3. EXCEPCIONES DE SEGURIDAD Y AUTENTICACIÓN
    // =========================================================================

    /**
     * Maneja las excepciones de credenciales incorrectas durante el login.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,
                "Credenciales incorrectas. Verifique su correo y contraseña.",
                "INVALID_CREDENTIALS");
    }

    /**
     * Maneja las excepciones de usuario no encontrado durante el proceso de autenticación.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 401 Unauthorized
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,
                "No se encontró una cuenta con esas credenciales.",
                "USER_NOT_FOUND");
    }

    /**
     * Maneja los casos en que el usuario está autenticado, pero no tiene
     * los roles o permisos suficientes para realizar la operación solicitada.
     *
     * @param ex la excepción de acceso denegado
     * @return respuesta HTTP 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta operación.",
                "ACCESS_DENIED"
        );
    }

    // =========================================================================
    // 4. EXCEPCIONES DE CARGA DE ARCHIVOS (MULTIPART / CLOUDINARY)
    // =========================================================================

    /**
     * Maneja solicitudes que exceden el tamaño máximo de archivo permitido.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "El archivo supera el tamaño máximo permitido (5 MB).",
                "FILE_TOO_LARGE"
        );
    }

    /**
     * Maneja solicitudes multipart mal formadas o que no se pueden analizar.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException ex) {
        log.warn("Solicitud multipart inválida o incompleta: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "La solicitud de carga de imagen está incompleta o mal formada.",
                "INVALID_MULTIPART_REQUEST"
        );
    }

    /**
     * Maneja solicitudes que omiten la parte multipart requerida (ej. campo 'file').
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Debes adjuntar una imagen usando el campo 'file'.",
                "MISSING_FILE_PART"
        );
    }

    /**
     * Maneja solicitudes que no incluyen un parámetro de consulta/ruta requerido.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Falta un parámetro requerido en la solicitud: " + ex.getParameterName(),
                "MISSING_PARAMETER"
        );
    }

    /**
     * Maneja solicitudes con un Content-Type no soportado (ej. enviar JSON en lugar de multipart/form-data).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El contenido debe enviarse como multipart/form-data e incluir el archivo en el campo 'file'.",
                "UNSUPPORTED_MEDIA_TYPE"
        );
    }

    /**
     * Maneja fallos específicos durante la carga de imágenes en Cloudinary.
     */
    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<Map<String, Object>> handleCloudinaryUploadException(CloudinaryUploadException ex) {
        log.error("Error durante la carga de una imagen en Cloudinary", ex);
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "No fue posible procesar la imagen con el servicio de almacenamiento.",
                "CLOUDINARY_UPLOAD_FAILED"
        );
    }

    /**
     * Maneja fallos específicos durante la eliminación de imágenes en Cloudinary.
     */
    @ExceptionHandler(CloudinaryDeleteException.class)
    public ResponseEntity<Map<String, Object>> handleCloudinaryDeleteException(CloudinaryDeleteException ex) {
        log.error("Error durante la eliminación de una imagen en Cloudinary", ex);
        return buildErrorResponse(
                HttpStatus.BAD_GATEWAY,
                "No fue posible eliminar la imagen del servicio de almacenamiento. El registro de la galería se conservó.",
                "CLOUDINARY_DELETE_FAILED"
        );
    }

    // =========================================================================
    // 5. EXCEPCIONES GENERALES (FALLBACK)
    // =========================================================================

    /**
     * Maneja todas las excepciones no capturadas específicamente por los manejadores anteriores.
     *
     * @param ex la excepción lanzada
     * @return respuesta HTTP 500 Internal Server Error con mensaje genérico
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Error inesperado en el servidor", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado en el servidor. Por favor, intente más tarde.",
                "INTERNAL_ERROR"
        );
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Construye una respuesta de error estandarizada en formato JSON.
     *
     * @param status    el código de estado HTTP
     * @param message   el mensaje descriptivo del error para el cliente
     * @param errorCode el código identificador interno del error
     * @return la respuesta HTTP con el cuerpo de error estandarizado
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, String errorCode) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("errorCode", errorCode);

        return ResponseEntity.status(status).body(errorResponse);
    }
}
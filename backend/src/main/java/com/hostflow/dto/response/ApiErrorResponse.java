package com.hostflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cuerpo JSON estándar de error de la API HostFlow (seguridad y {@code GlobalExceptionHandler}).
 */
@Schema(name = "ApiErrorResponse", description = "Error estandarizado de la API")
public record ApiErrorResponse(
        @Schema(description = "Momento del error", example = "2026-11-10T20:00:00")
        String timestamp,
        @Schema(description = "Código HTTP", example = "400")
        int status,
        @Schema(description = "Frase del estado HTTP", example = "Bad Request")
        String error,
        @Schema(description = "Mensaje legible para el cliente", example = "Las fechas seleccionadas no están disponibles.")
        String message,
        @Schema(description = "Código interno de error", example = "BUSINESS_ERROR")
        String errorCode,
        @Schema(description = "Ruta solicitada (presente en errores de seguridad)", example = "/owners/reservations", nullable = true)
        String path
) {}

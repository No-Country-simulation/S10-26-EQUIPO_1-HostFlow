package com.hostflow.controller;

import com.hostflow.service.OwnerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para el dashboard del propietario (Owner).
 *
 * <p>Expone endpoints para que los propietarios consulten las métricas
 * operativas y financieras de sus alojamientos en tiempo real.</p>
 *
 * <p><strong>Seguridad:</strong> Todos los endpoints requieren autenticación
 * válida mediante JWT y el rol {@code ROLE_OWNER}.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/owners/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Dashboard Owner", description = "Métricas y estadísticas para el propietario")
@Slf4j
public class OwnerDashboardController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula toda la lógica de negocio y consultas
     * relacionadas con las métricas del dashboard.
     * El controlador NO debe inyectar repositorios directamente.
     */
    private final OwnerDashboardService ownerDashboardService;

    // =========================================================================
    // ENDPOINTS
    // =========================================================================

    /**
     * Obtiene las métricas consolidadas del alojamiento del propietario autenticado.
     *
     * <p>La información incluye el volumen de reservas (activas y completadas)
     * y los ingresos (del mes actual y acumulados históricos).</p>
     *
     * @param auth el contexto de autenticación de Spring Security (contiene el email del usuario en el campo "sub")
     * @return respuesta HTTP 200 OK con un mapa de las métricas solicitadas
     */
    @GetMapping("/metrics")
    @Operation(
            summary = "Obtener métricas del dashboard del propietario",
            description = "Devuelve un resumen de reservas activas, completadas e ingresos (mensuales y acumulados) " +
                    "del alojamiento asociado al propietario autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas obtenidas exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de propietario (se requiere ROLE_OWNER)"),
            @ApiResponse(responseCode = "404", description = "El propietario no tiene un alojamiento registrado aún")
    })
    public ResponseEntity<Map<String, Object>> getOwnerMetrics(Authentication auth) {
        // 1. Validar y extraer el email del usuario autenticado desde el token JWT
        if (auth == null || auth.getName() == null) {
            log.warn("⚠️ Intento de acceso al dashboard sin autenticación válida.");
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Se requiere autenticación válida para acceder al dashboard."
            );
        }

        String ownerEmail = auth.getName();
        log.debug("📊 Solicitud de métricas del dashboard para el email: {}", ownerEmail);

        // 2. Delegar la lógica de negocio y consultas al servicio
        // Nota: Si el owner no existe o no tiene alojamiento, el servicio lanzará ResourceNotFoundException,
        // la cual será atrapada y manejada elegantemente por el GlobalExceptionHandler.
        Map<String, Object> metrics = ownerDashboardService.getOwnerMetrics(ownerEmail);

        // 3. Retornar la respuesta HTTP estandarizada
        return ResponseEntity.ok(metrics);
    }
}
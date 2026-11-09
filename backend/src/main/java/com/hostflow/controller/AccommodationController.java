package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationDisableRequest;
import com.hostflow.dto.response.AccommodationDisableResponse;
import com.hostflow.model.AccommodationTemporaryDisable;
import com.hostflow.service.AccommodationTemporaryDisableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de alojamientos por parte de los propietarios.
 *
 * <p>Permite a los dueños gestionar la disponibilidad temporal de su alojamiento
 * (desactivar por remodelación, mantenimiento, vacaciones, etc.).</p>
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.0):</strong></p>
 * <ul>
 *   <li>Se eliminó la inyección directa de {@code OwnerRepository}.</li>
 *   <li>El controlador ahora solo extrae el email del usuario autenticado
 *       y lo delega al servicio, manteniendo la separación de responsabilidades.</li>
 *   <li>Se eliminó el método privado {@code getCurrentOwnerId()} que consultaba la BD.</li>
 * </ul>
 *
 * <p><strong>Seguridad:</strong> Todos los endpoints requieren autenticación
 * y el rol {@code ROLE_OWNER}. Cada owner solo puede gestionar SU PROPIO alojamiento.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/owners/accommodation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestión de Alojamiento", description = "Endpoints para que los owners gestionen su alojamiento")
@SecurityRequirement(name = "Bearer Authentication")
public class AccommodationController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula toda la lógica de negocio relacionada con
     * la desactivación temporal de alojamientos.
     *
     * <p>El servicio se encarga de resolver el Owner a partir del email,
     * evitando que este controlador necesite inyectar repositorios directamente.</p>
     */
    private final AccommodationTemporaryDisableService disableService;

    // =========================================================================
    // ENDPOINTS
    // =========================================================================

    // --------------------------------------------------------------------------
    // 1. DESACTIVAR ALOJAMIENTO
    // --------------------------------------------------------------------------

    /**
     * Desactiva temporalmente el alojamiento del owner autenticado.
     *
     * @param request los datos de la desactivación
     * @param authentication la autenticación del usuario actual
     * @return la desactivación creada
     */
    @PostMapping("/disable")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Desactivar temporalmente mi alojamiento",
            description = """
                    Permite al owner desactivar temporalmente su alojamiento por motivos como:
                    - Remodelación
                    - Mantenimiento
                    - Vacaciones
                    - Cierre temporal
                    El alojamiento no estará disponible para reservas durante el período especificado.
                    Solo afecta al alojamiento del owner autenticado.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Alojamiento desactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Ya existe una desactivación activa o fechas inválidas"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Owner no encontrado")
    })
    public ResponseEntity<AccommodationDisableResponse> disableAccommodation(
            @Valid @RequestBody AccommodationDisableRequest request,
            Authentication authentication) {

        // ✅ Extraer email del usuario autenticado (sin consultar BD)
        String ownerEmail = authentication.getName();
        log.debug("🔒 Solicitando desactivación de alojamiento para email: {}", ownerEmail);

        // ✅ Delegar toda la lógica al servicio
        AccommodationTemporaryDisable disable = disableService.disableAccommodationByEmail(
                ownerEmail,
                request.getReason(),
                request.getDisabledFrom(),
                request.getDisabledUntil()
        );

        return ResponseEntity.status(201).body(mapToResponse(disable));
    }

    // --------------------------------------------------------------------------
    // 2. REACTIVAR ALOJAMIENTO
    // --------------------------------------------------------------------------

    /**
     * Reactiva el alojamiento del owner autenticado.
     */
    @PostMapping("/enable")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Reactivar mi alojamiento",
            description = "Reactiva el alojamiento del owner, haciéndolo disponible nuevamente para reservas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alojamiento reactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No hay desactivación activa"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<AccommodationDisableResponse> enableAccommodation(Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("🔓 Solicitando reactivación de alojamiento para email: {}", ownerEmail);

        AccommodationTemporaryDisable disable = disableService.enableAccommodationByEmail(ownerEmail);
        return ResponseEntity.ok(mapToResponse(disable));
    }

    // --------------------------------------------------------------------------
    // 3. CONSULTAR ESTADO
    // --------------------------------------------------------------------------

    /**
     * Obtiene el estado actual de desactivación del alojamiento.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Consultar estado de mi alojamiento",
            description = "Verifica si el alojamiento está actualmente desactivado temporalmente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<AccommodationDisableResponse> getAccommodationStatus(Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("📊 Consultando estado de alojamiento para email: {}", ownerEmail);

        AccommodationTemporaryDisable disable = disableService.getActiveDisableByEmail(ownerEmail);
        if (disable == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(mapToResponse(disable));
    }

    // --------------------------------------------------------------------------
    // 4. HISTORIAL
    // --------------------------------------------------------------------------

    /**
     * Obtiene el historial de desactivaciones del alojamiento.
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Historial de desactivaciones de mi alojamiento",
            description = "Obtiene todas las desactivaciones temporales aplicadas al alojamiento del owner."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<List<AccommodationDisableResponse>> getAccommodationHistory(Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("📜 Obteniendo historial de desactivaciones para email: {}", ownerEmail);

        List<AccommodationTemporaryDisable> history = disableService.getDisableHistoryByEmail(ownerEmail);
        List<AccommodationDisableResponse> responses = history.stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // --------------------------------------------------------------------------
    // 5. DISPONIBILIDAD EN FECHA
    // --------------------------------------------------------------------------

    /**
     * Verifica si el alojamiento está disponible en una fecha específica.
     */
    @GetMapping("/availability")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Verificar disponibilidad en fecha específica",
            description = "Consulta si el alojamiento estará disponible en una fecha determinada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad consultada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam java.time.LocalDateTime date,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("📅 Verificando disponibilidad para email: {} en fecha: {}", ownerEmail, date);

        boolean isAvailable = disableService.isAccommodationAvailableByEmail(ownerEmail, date);
        return ResponseEntity.ok(isAvailable);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Mapea la entidad {@link AccommodationTemporaryDisable} a su DTO de respuesta.
     *
     * @param disable la entidad a mapear
     * @return el DTO de respuesta
     */
    private AccommodationDisableResponse mapToResponse(AccommodationTemporaryDisable disable) {
        return AccommodationDisableResponse.builder()
                .id(disable.getId())
                .ownerId(disable.getOwnerId())
                .reason(disable.getReason())
                .disabledFrom(disable.getDisabledFrom())
                .disabledUntil(disable.getDisabledUntil())
                .isActive(disable.getIsActive())
                .createdAt(disable.getCreatedAt())
                .build();
    }
}
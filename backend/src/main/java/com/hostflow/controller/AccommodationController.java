package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationDisableRequest;
import com.hostflow.dto.response.AccommodationDisableResponse;
import com.hostflow.model.AccommodationTemporaryDisable;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
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
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de alojamientos por parte de los propietarios.
 *
 * <p>Permite a los dueños gestionar la disponibilidad temporal de su alojamiento
 * (desactivar por remodelación, mantenimiento, vacaciones, etc.).</p>
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

    private final AccommodationTemporaryDisableService disableService;

    /**  Repositorio para obtener el ID del owner autenticado */
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // DESACTIVAR ALOJAMIENTO
    // =========================================================================

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

        Long ownerId = getCurrentOwnerId(authentication);

        AccommodationTemporaryDisable disable = disableService.disableAccommodation(
                ownerId,
                request.getReason(),
                request.getDisabledFrom(),
                request.getDisabledUntil()
        );

        return ResponseEntity.status(201).body(mapToResponse(disable));
    }

    // =========================================================================
    // REACTIVAR ALOJAMIENTO
    // =========================================================================

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
        Long ownerId = getCurrentOwnerId(authentication);

        AccommodationTemporaryDisable disable = disableService.enableAccommodation(ownerId);

        return ResponseEntity.ok(mapToResponse(disable));
    }

    // =========================================================================
    // CONSULTAR ESTADO
    // =========================================================================

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
        Long ownerId = getCurrentOwnerId(authentication);

        AccommodationTemporaryDisable disable = disableService.getActiveDisable(ownerId);

        if (disable == null) {
            return ResponseEntity.ok(null);
        }

        return ResponseEntity.ok(mapToResponse(disable));
    }

    // =========================================================================
    // HISTORIAL
    // =========================================================================

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
        Long ownerId = getCurrentOwnerId(authentication);

        List<AccommodationTemporaryDisable> history = disableService.getDisableHistory(ownerId);

        List<AccommodationDisableResponse> responses = history.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // =========================================================================
    // DISPONIBILIDAD EN FECHA
    // =========================================================================

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

        Long ownerId = getCurrentOwnerId(authentication);
        boolean isAvailable = disableService.isAccommodationAvailable(ownerId, date);

        return ResponseEntity.ok(isAvailable);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * 🆕 Obtiene el ID del owner actualmente autenticado.
     *
     * <p>Extrae el email del usuario autenticado desde el contexto de seguridad
     * de Spring Security (que proviene del token JWT) y busca el owner correspondiente
     * en la base de datos.</p>
     *
     * <p><strong>Flujo:</strong></p>
     * <ol>
     *   <li>El token JWT contiene el email del usuario en el campo "sub" (subject)</li>
     *   <li>Spring Security lo expone a través de {@code authentication.getName()}</li>
     *   <li>Buscamos el owner por ese email en la base de datos</li>
     *   <li>Retornamos su ID para usarlo en las operaciones</li>
     * </ol>
     *
     * @param authentication el contexto de autenticación de Spring Security
     * @return el ID del owner autenticado
     * @throws RuntimeException si no se encuentra el owner (no debería ocurrir si está autenticado)
     */
    private Long getCurrentOwnerId(Authentication authentication) {
        // El email del usuario autenticado (extraído del token JWT)
        String email = authentication.getName();

        log.debug("🔍 Obteniendo owner ID para email: {}", email);

        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró el owner asociado al email autenticado: " + email));

        log.debug("✅ Owner ID obtenido: {} (email: {})", owner.getId(), email);

        return owner.getId();
    }

    /**
     * Mapea la entidad a DTO de respuesta.
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
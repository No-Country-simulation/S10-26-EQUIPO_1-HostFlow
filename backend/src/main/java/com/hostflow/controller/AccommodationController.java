package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationDisableRequest;
import com.hostflow.dto.response.AccommodationDisableResponse;
import com.hostflow.model.AccommodationTemporaryDisable;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.service.AccommodationTemporaryDisableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de alojamientos por parte de los propietarios.
 *
 * <p>Centraliza los endpoints que permiten a los dueños (Owners) gestionar la
 * disponibilidad temporal de su alojamiento (por motivos de remodelación,
 * mantenimiento, vacaciones o cierre temporal).</p>
 *
 * <p><strong>Consideraciones de Seguridad:</strong></p>
 * <ul>
 *   <li>Todos los endpoints están protegidos y requieren autenticación JWT válida.</li>
 *   <li>Se aplica la anotación {@code @PreAuthorize("hasRole('OWNER')")} para garantizar
 *       que solo usuarios con este rol puedan acceder.</li>
 *   <li>El aislamiento de datos está garantizado: cada owner solo puede gestionar
 *       y consultar la información de SU PROPIO alojamiento, nunca el de otro usuario.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@RestController
@RequestMapping("/owners/accommodation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestión de Alojamiento", description = "Endpoints para que los owners gestionen la disponibilidad de su alojamiento")
@SecurityRequirement(name = "Bearer Authentication")
public class AccommodationController {

    /**
     * Servicio de negocio para gestionar la desactivación temporal de alojamientos.
     * Inyectado de forma inmutable vía constructor gracias a {@link RequiredArgsConstructor}.
     */
    private final AccommodationTemporaryDisableService disableService;

    /**
     * Repositorio para obtener los datos del owner autenticado.
     * Se utiliza para resolver el ID del usuario a partir del email extraído del contexto de seguridad.
     */
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // 1. DESACTIVAR ALOJAMIENTO
    // =========================================================================

    /**
     * Desactiva temporalmente el alojamiento del owner autenticado.
     *
     * @param request        DTO con los datos de la desactivación (motivo, fecha inicio, fecha fin).
     * @param authentication Contexto de seguridad de Spring que contiene los datos del usuario logueado.
     * @return Respuesta HTTP 201 (Created) con el registro de desactivación persistido.
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
                    
                    El alojamiento no estará disponible para nuevas reservas durante el período especificado.
                    Solo afecta al alojamiento del owner autenticado.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Alojamiento desactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Ya existe una desactivación activa o las fechas son inválidas"),
            @ApiResponse(responseCode = "401", description = "No autenticado (Token JWT faltante o inválido)"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Owner no encontrado en la base de datos")
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
    // 2. REACTIVAR ALOJAMIENTO
    // =========================================================================

    /**
     * Reactiva el alojamiento del owner autenticado, marcando la desactivación
     * actual como inactiva.
     *
     * @param authentication Contexto de seguridad de Spring.
     * @return Respuesta HTTP 200 (OK) con el registro de desactivación actualizado.
     */
    @PostMapping("/enable")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Reactivar mi alojamiento",
            description = "Reactiva el alojamiento del owner, haciéndolo disponible nuevamente para reservas " +
                    "antes de la fecha de fin estimada (si la hubiera)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alojamiento reactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No hay ninguna desactivación activa para revertir"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<AccommodationDisableResponse> enableAccommodation(Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);

        AccommodationTemporaryDisable disable = disableService.enableAccommodation(ownerId);

        return ResponseEntity.ok(mapToResponse(disable));
    }

    // =========================================================================
    // 3. CONSULTAR ESTADO ACTUAL
    // =========================================================================

    /**
     * Obtiene el estado actual de desactivación del alojamiento.
     *
     * @param authentication Contexto de seguridad de Spring.
     * @return Respuesta HTTP 200 (OK) con la desactivación activa, o null si el alojamiento está operativo.
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

        // Retornamos null intencionalmente si no hay desactivación, lo cual es válido en este contexto
        if (disable == null) {
            return ResponseEntity.ok(null);
        }

        return ResponseEntity.ok(mapToResponse(disable));
    }

    // =========================================================================
    // 4. HISTORIAL DE DESACTIVACIONES
    // =========================================================================

    /**
     * Obtiene el historial completo de desactivaciones del alojamiento.
     *
     * @param authentication Contexto de seguridad de Spring.
     * @return Respuesta HTTP 200 (OK) con una lista de desactivaciones, ordenadas de la más reciente a la más antigua.
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Historial de desactivaciones de mi alojamiento",
            description = "Obtiene todas las desactivaciones temporales (activas e inactivas) aplicadas al alojamiento del owner."
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
    // 5. DISPONIBILIDAD EN FECHA ESPECÍFICA (Actualizado)
    // =========================================================================

    /**
     * Verifica si el alojamiento está disponible en una fecha específica.
     *
     * <p>Este endpoint está diseñado para recibir únicamente una fecha (sin hora),
     * lo que mejora la experiencia de usuario en Swagger UI y en el frontend.</p>
     *
     * <p><strong>Características de la implementación:</strong></p>
     * <ul>
     *   <li><strong>Formato:</strong> Acepta {@code LocalDate} (yyyy-MM-dd).</li>
     *   <li><strong>UX en Swagger:</strong> Muestra un selector de fecha nativo del navegador que
     *       autocompleta los guiones automáticamente al escribir (año-mes-día).</li>
     *   <li><strong>Lógica de negocio:</strong> Convierte internamente la fecha a un rango de
     *       {@code LocalDateTime} (00:00:00 a 23:59:59) para verificar si existe alguna
     *       superposición con una desactivación activa en la base de datos.</li>
     * </ul>
     *
     * @param date           La fecha a consultar (ej: 2026-10-27).
     * @param authentication Contexto de seguridad de Spring.
     * @return {@code true} si el alojamiento está disponible, {@code false} si está desactivado ese día.
     */
    @GetMapping("/availability")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Verificar disponibilidad en fecha específica",
            description = """
                    Consulta si el alojamiento estará disponible en una fecha determinada.
                    
                    Formato de fecha: yyyy-MM-dd (ej: 2026-10-27)
                    
                    <strong>Características:</strong>
                    - Swagger UI muestra un selector de fecha con autocompletado de guiones.
                    - Valida el formato de fecha automáticamente.
                    - Verifica disponibilidad para todo el día completo (00:00:00 a 23:59:59).
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad consultada exitosamente (true/false)"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<Boolean> checkAvailability(
            @Parameter(
                    description = "Fecha a consultar (formato: yyyy-MM-dd). " +
                            "Swagger autocompleta los guiones automáticamente al escribir.",
                    example = "2026-10-27",
                    required = true
            )
            @RequestParam LocalDate date,
            Authentication authentication) {

        Long ownerId = getCurrentOwnerId(authentication);

        // Conversión segura: LocalDate -> LocalDateTime (inicio del día: 00:00:00)
        // El servicio se encarga de expandir esto al rango completo del día para la consulta a la BD.
        boolean isAvailable = disableService.isAccommodationAvailable(
                ownerId,
                date.atStartOfDay()
        );

        return ResponseEntity.ok(isAvailable);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Obtiene el ID del owner actualmente autenticado.
     *
     * <p><strong>Flujo de resolución de identidad:</strong></p>
     * <ol>
     *   <li>El token JWT contiene el email del usuario en el campo "sub" (subject).</li>
     *   <li>Spring Security expone este valor a través de {@code authentication.getName()}.</li>
     *   <li>Se consulta la base de datos para obtener la entidad {@link Owner} completa.</li>
     *   <li>Se retorna el ID numérico para usarlo como clave foránea en las operaciones de negocio.</li>
     * </ol>
     *
     * @param authentication El contexto de autenticación de Spring Security.
     * @return El ID numérico del owner autenticado.
     * @throws RuntimeException Si no se encuentra el owner (esto indicaría una inconsistencia
     *                          entre el token JWT y la base de datos).
     */
    private Long getCurrentOwnerId(Authentication authentication) {
        String email = authentication.getName();

        log.debug("🔍 Resolviendo Owner ID para el email autenticado: {}", email);

        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ Inconsistencia de seguridad: Token válido pero owner no existe en BD. Email: {}", email);
                    return new RuntimeException("No se encontró el owner asociado al email autenticado: " + email);
                });

        log.debug("✅ Owner ID resuelto exitosamente: {} (email: {})", owner.getId(), email);

        return owner.getId();
    }

    /**
     * Mapea la entidad de JPA a su representación DTO (Data Transfer Object).
     *
     * <p>Este patrón es crucial para:
     * <ul>
     *   <li>Evitar la exposición de datos sensibles o internos de la entidad.</li>
     *   <li>Controlar exactamente qué campos se serializan en la respuesta JSON.</li>
     *   <li>Desacoplar la capa de persistencia de la capa de presentación.</li>
     * </ul>
     *
     * @param disable La entidad {@link AccommodationTemporaryDisable} a mapear.
     * @return El DTO {@link AccommodationDisableResponse} listo para la respuesta HTTP.
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
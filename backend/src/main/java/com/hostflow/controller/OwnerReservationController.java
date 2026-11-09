package com.hostflow.controller;

import com.hostflow.dto.request.CreateReservationRequest;
import com.hostflow.dto.response.ReservationLinkResponse;
import com.hostflow.dto.response.ReservationResponse;
import com.hostflow.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para que los propietarios gestionen las reservas
 * de sus alojamientos en la protected plataforma HostFlow.
 *
 * <p><strong>🆕 Mejora de arquitectura y seguridad (v2.1):</strong></p>
 * <ul>
 *   <li>Se eliminó la inyección directa de {@code OwnerRepository} y {@code AccommodationRepository}.</li>
 *   <li>Se eliminó el método privado {@code getCurrentOwnerId()} que consultaba la BD.</li>
 *   <li>🛡️ <strong>Corrección de seguridad (IDOR):</strong> Todos los endpoints ahora validan estrictamente
 *       que la reserva o la consulta de disponibilidad pertenezca al alojamiento del propietario autenticado,
 *       evitando que un owner manipule datos de otros alojamientos.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@RestController
@RequestMapping("/owners/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Gestión de Reservas (Owner)",
        description = "Endpoints para que los propietarios gestionen las reservas de sus alojamientos")
@Slf4j
public class OwnerReservationController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula toda la lógica de negocio de las reservas.
     * Se encarga de validar que el propietario tenga derecho a operar
     * sobre la reserva o alojamiento solicitado.
     */
    private final ReservationService reservationService;

    // =========================================================================
    // 1. CREAR RESERVA
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Crear nueva reserva",
            description = """
            Crea una nueva reserva para el alojamiento del owner autenticado.
            
            Proceso:
            1. Valida disponibilidad del alojamiento en las fechas solicitadas.
            2. Verifica que el huésped tenga al menos un método de pago activo.
            3. Genera un enlace único de confirmación válido por 24 horas (configurable).
            4. El enlace debe ser enviado al huésped por email.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas, huésped sin método de pago o alojamiento no disponible"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner no tiene un alojamiento registrado")
    })
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.info("🏨 Creando nueva reserva para owner email: {}", ownerEmail);

        ReservationResponse response = reservationService.createReservationByEmail(ownerEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // 2. LISTAR MIS RESERVAS
    // =========================================================================

    @GetMapping
    @Operation(
            summary = "Listar mis reservas",
            description = "Obtiene todas las reservas del alojamiento del owner autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner no tiene un alojamiento registrado")
    })
    public ResponseEntity<List<ReservationResponse>> listMyReservations(Authentication authentication) {
        String ownerEmail = authentication.getName();
        log.debug("📋 Listando reservas para owner email: {}", ownerEmail);

        List<ReservationResponse> responses = reservationService.listMyReservationsByEmail(ownerEmail);
        return ResponseEntity.ok(responses);
    }

    // =========================================================================
    // 3. OBTENER RESERVA POR ID (CON VALIDACIÓN DE PROPIEDAD)
    // =========================================================================

    @GetMapping("/{reservationId}")
    @Operation(
            summary = "Obtener reserva por ID",
            description = "Obtiene los detalles de una reserva específica, validando que pertenezca al alojamiento del owner."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada o no pertenece a tu alojamiento")
    })
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("🔍 Obteniendo reserva ID: {} para owner email: {}", reservationId, ownerEmail);

        ReservationResponse response = reservationService.getReservationByIdAndOwner(reservationId, ownerEmail);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 4. CANCELAR RESERVA (CON VALIDACIÓN DE PROPIEDAD)
    // =========================================================================

    @PostMapping("/{reservationId}/cancel")
    @Operation(
            summary = "Cancelar reserva",
            description = "Cancela una reserva existente, validando que pertenezca al alojamiento del owner."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva cancelada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La reserva no se puede cancelar (ya está completada o cancelada)"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada o no pertenece a tu alojamiento")
    })
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long reservationId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.info("❌ Cancelando reserva ID: {} para owner email: {}", reservationId, ownerEmail);

        ReservationResponse response = reservationService.cancelReservationByIdAndOwner(reservationId, reason, ownerEmail);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 5. REACTIVAR ENLACE (CON VALIDACIÓN DE PROPIEDAD)
    // =========================================================================

    @PostMapping("/{reservationId}/reactivate-link")
    @Operation(
            summary = "Reactivar enlace de confirmación",
            description = "Reactiva un enlace de confirmación que ha expirado, validando la propiedad de la reserva."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enlace reactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "La reserva no está en estado PENDING"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada o no pertenece a tu alojamiento")
    })
    public ResponseEntity<ReservationLinkResponse> reactivateLink(
            @PathVariable Long reservationId,
            @RequestParam(defaultValue = "24") int expirationHours,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.info("🔄 Reactivando enlace para reserva ID: {} de owner email: {}", reservationId, ownerEmail);

        ReservationLinkResponse response = reservationService.reactivateLinkByIdAndOwner(reservationId, expirationHours, ownerEmail);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 6. VERIFICAR DISPONIBILIDAD (CORREGIDO: SIN accommodationId EN LA URL)
    // =========================================================================

    @GetMapping("/check-availability")
    @Operation(
            summary = "Verificar disponibilidad de mi alojamiento",
            description = "Verifica si el alojamiento del owner autenticado está disponible en las fechas especificadas. " +
                    "Ya no requiere accommodationId en la URL para prevenir consultas a alojamientos ajenos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad verificada (true/false)"),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas"),
            @ApiResponse(responseCode = "404", description = "El owner no tiene un alojamiento registrado")
    })
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("📅 Verificando disponibilidad para owner email: {} entre {} y {}", ownerEmail, checkIn, checkOut);

        // 🛡️ SEGURIDAD: El servicio deriva el accommodationId internamente desde el ownerEmail.
        boolean available = reservationService.checkAvailabilityByEmail(ownerEmail, checkIn, checkOut);
        return ResponseEntity.ok(available);
    }

    // =========================================================================
    // 7. COMPLETAR RESERVA (CON VALIDACIÓN DE PROPIEDAD)
    // =========================================================================

    @PostMapping("/{reservationId}/complete")
    @Operation(
            summary = "Completar reserva",
            description = "Marca una reserva como COMPLETADA después del check-out, validando que pertenezca al owner."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva completada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La reserva no puede ser completada (no está CONFIRMED o el check-out no ha pasado)"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada o no pertenece a tu alojamiento")
    })
    public ResponseEntity<ReservationResponse> completeReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.info("✅ Completando reserva ID: {} para owner email: {}", reservationId, ownerEmail);

        ReservationResponse response = reservationService.completeReservationByIdAndOwner(reservationId, ownerEmail);
        return ResponseEntity.ok(response);
    }
}
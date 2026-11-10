package com.hostflow.controller;

import com.hostflow.dto.request.CreateReservationRequest;
import com.hostflow.dto.response.ReservationLinkResponse;
import com.hostflow.dto.response.ReservationResponse;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para que los propietarios gestionen las reservas de sus alojamientos.
 *
 * <ul>
 *   <li>Crear reservas y enviar el enlace de onboarding (24 h fijas).</li>
 *   <li>Listar y consultar reservas del propio alojamiento.</li>
 *   <li>Registrar pago recibido y cancelar reservas.</li>
 *   <li>Renovar el enlace de onboarding.</li>
 *   <li>Verificar disponibilidad del propio alojamiento.</li>
 *   <li>Marcar reservas como COMPLETED después del check-out.</li>
 * </ul>
 *
 * <p>Todas las operaciones se limitan al propietario autenticado y a su alojamiento.</p>
 *
 * @author HostFlow Team
 * @version 2.2.0 (Contrato Fase B: creación y renovación sin parámetros de expiración)
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

    private final ReservationService reservationService;
    private final OwnerRepository ownerRepository;
    private final AccommodationRepository accommodationRepository;

    // =========================================================================
    // 1. CREAR RESERVA
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Crear nueva reserva",
            description = """
            Crea una reserva para el alojamiento del propietario autenticado y envía al huésped
            el enlace de onboarding (vigencia fija de 24 horas).
            
            Proceso:
            1. Valida que el alojamiento esté habilitado para reservas (perfil, logo, galería y método de pago del alojamiento).
            2. Valida fechas, capacidad y disponibilidad (sin solapamiento de noches).
            3. Crea la reserva en estado PENDING y genera un enlace de onboarding de 24 horas.
            4. Envía el correo al huésped; si el envío falla, la operación se revierte.
            
            Notas:
            - Los métodos de pago informados al huésped son los del alojamiento, no del huésped.
            - Alergias, preferencias y servicios adicionales se capturan en el onboarding público.
            - Check-in puede ser hoy; check-out debe ser posterior. Las fechas usan formato YYYY-MM-DD.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas o alojamiento no disponible"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Owner no encontrado")
    })
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {

        Long ownerId = getCurrentOwnerId(authentication);
        ReservationResponse response = reservationService.createReservation(ownerId, request);

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
        Long ownerId = getCurrentOwnerId(authentication);

        // Obtener el alojamiento del owner
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tienes un alojamiento registrado. Registra los datos de tu alojamiento primero."));

        // Listar todas las reservas del alojamiento
        List<ReservationResponse> responses = reservationService.listReservationsByAccommodation(accommodation.getId());

        return ResponseEntity.ok(responses);
    }

    // =========================================================================
    // 3. OBTENER RESERVA POR ID
    // =========================================================================

    @GetMapping("/{reservationId}")
    @Operation(
            summary = "Obtener reserva por ID",
            description = "Obtiene los detalles de una reserva específica."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long reservationId,
                                                              Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        ReservationResponse response = reservationService.getReservationById(ownerId, reservationId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 4. REGISTRAR PAGO RECIBIDO
    // =========================================================================

    @PostMapping("/{reservationId}/mark-paid")
    @Operation(
            summary = "Marcar reserva como pagada",
            description = "Registra el pago después de que el propietario verifica el comprobante recibido por WhatsApp. Envía las confirmaciones por correo una sola vez."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago registrado; confirmaciones enviadas después del commit"),
            @ApiResponse(responseCode = "400", description = "La reserva no está confirmada o el huésped no completó sus datos"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> markReservationAsPaid(
            @PathVariable Long reservationId,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        ReservationResponse response = reservationService.markPaymentAsPaid(ownerId, reservationId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 4. CANCELAR RESERVA
    // =========================================================================

    @PostMapping("/{reservationId}/cancel")
    @Operation(
            summary = "Cancelar reserva",
            description = "Cancela una reserva existente. Solo se pueden cancelar reservas en estado PENDING o CONFIRMED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva cancelada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La reserva no se puede cancelar (ya está completada o cancelada)"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long reservationId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        ReservationResponse response = reservationService.cancelReservation(ownerId, reservationId, reason);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 5. REACTIVAR ENLACE
    // =========================================================================

    @PostMapping("/{reservationId}/reactivate-link")
    @Operation(
            summary = "Renovar enlace de onboarding",
            description = """
            Invalida los enlaces vigentes de la reserva y genera uno nuevo con vigencia fija de 24 horas.
            Solo aplica a reservas en estado PENDING. El correo al huésped se envía en modo estricto:
            si falla el envío, la renovación se revierte y se conserva el enlace anterior.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enlace renovado y correo aceptado por el proveedor"),
            @ApiResponse(responseCode = "400", description = "La reserva no está en estado PENDING o falló el envío del correo"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationLinkResponse> reactivateLink(
            @PathVariable Long reservationId,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        ReservationLinkResponse response = reservationService.reactivateLink(ownerId, reservationId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 6. VERIFICAR DISPONIBILIDAD
    // =========================================================================

    @GetMapping("/check-availability")
    @Operation(
            summary = "Verificar disponibilidad",
            description = "Verifica la disponibilidad del alojamiento del propietario autenticado. Las fechas usan formato YYYY-MM-DD; check-out no ocupa esa noche."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad verificada (true/false)"),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Owner o alojamiento no encontrado")
    })
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkIn,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOut,
            Authentication authentication) {

        Long ownerId = getCurrentOwnerId(authentication);
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tienes un alojamiento registrado. Registra los datos de tu alojamiento primero."));

        boolean available = reservationService.isAccommodationAvailable(
                accommodation.getId(), checkIn, checkOut);
        return ResponseEntity.ok(available);
    }

    // =========================================================================
    // 7. 🆕 COMPLETAR RESERVA (REGISTRO DE INGRESOS)
    // =========================================================================

    @PostMapping("/{reservationId}/complete")
    @Operation(
            summary = "Completar reserva",
            description = """
            Marca una reserva como COMPLETADA después del check-out del huésped.
            
            Esta acción es irreversible y es la que permite que los ingresos de esta 
            reserva se reflejen en las métricas del dashboard del propietario 
            (incomeLastMonth y accumulatedIncome).
            
            Validaciones:
            - La reserva debe estar en estado CONFIRMED.
            - La fecha de check-out ya debe haber pasado.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva completada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La reserva no puede ser completada (no está CONFIRMED o el check-out no ha pasado)"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> completeReservation(
            @PathVariable Long reservationId,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        ReservationResponse response = reservationService.completeReservation(ownerId, reservationId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Obtiene el ID del owner actualmente autenticado a partir del contexto de seguridad.
     *
     * @param authentication el contexto de autenticación de Spring Security
     * @return el ID del owner autenticado
     * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException si no hay autenticación
     * @throws ResourceNotFoundException si el owner no existe en la base de datos
     */
    private Long getCurrentOwnerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Se requiere autenticación válida.");
        }

        Owner owner = ownerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario autenticado con el email: " + authentication.getName()));

        return owner.getId();
    }
}
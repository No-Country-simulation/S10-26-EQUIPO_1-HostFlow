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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para que los propietarios gestionen las reservas
 * de sus alojamientos.
 *
 * <p>Permite a los owners:</p>
 * <ul>
 *   <li>Crear nuevas reservas generando enlaces de confirmación.</li>
 *   <li>Consultar el estado de las reservas.</li>
 *   <li>Cancelar reservas.</li>
 *   <li>Reactivar enlaces expirados.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
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
    private final AccommodationRepository accommodationRepository; // ✅ FALTABA ESTA

    // =========================================================================
    // CREAR RESERVA
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
            
            Validaciones:
            - No se permiten reservas overlapping (doble booking).
            - El huésped debe tener métodos de pago activos.
            - Las fechas deben ser futuras y válidas.
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
    // LISTAR MIS RESERVAS
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
    public ResponseEntity<List<ReservationResponse>> listMyReservations(
            Authentication authentication) {

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
    // OBTENER RESERVA POR ID
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
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable Long reservationId) {

        ReservationResponse response = reservationService.getReservationById(reservationId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // CANCELAR RESERVA
    // =========================================================================

    @PostMapping("/{reservationId}/cancel")
    @Operation(
            summary = "Cancelar reserva",
            description = "Cancela una reserva existente. Solo se pueden cancelar reservas en estado PENDING o CONFIRMED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva cancelada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La reserva no se puede cancelar"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long reservationId,
            @RequestParam(required = false) String reason) {

        ReservationResponse response = reservationService.cancelReservation(reservationId, reason);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // REACTIVAR ENLACE
    // =========================================================================

    @PostMapping("/{reservationId}/reactivate-link")
    @Operation(
            summary = "Reactivar enlace de confirmación",
            description = """
            Reactiva un enlace de confirmación que ha expirado.
            Genera un nuevo token y extiende la validez por las horas especificadas.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enlace reactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "La reserva no está en estado PENDING"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationLinkResponse> reactivateLink(
            @PathVariable Long reservationId,
            @RequestParam(defaultValue = "24") int expirationHours) {

        ReservationLinkResponse response = reservationService.reactivateLink(
                reservationId, expirationHours);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // VERIFICAR DISPONIBILIDAD
    // =========================================================================

    @GetMapping("/check-availability")
    @Operation(
            summary = "Verificar disponibilidad",
            description = "Verifica si el alojamiento está disponible en las fechas especificadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad verificada"),
            @ApiResponse(responseCode = "404", description = "Alojamiento no encontrado")
    })
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam Long accommodationId,
            @RequestParam java.time.LocalDateTime checkIn,
            @RequestParam java.time.LocalDateTime checkOut) {

        boolean available = reservationService.isAccommodationAvailable(
                accommodationId, checkIn, checkOut);
        return ResponseEntity.ok(available);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Obtiene el ID del owner actualmente autenticado.
     *
     * @param authentication el contexto de autenticación de Spring Security
     * @return el ID del owner autenticado
     */
    private Long getCurrentOwnerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Se requiere autenticación.");
        }
        Owner owner = ownerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario autenticado."));
        return owner.getId();
    }
}
package com.hostflow.controller;

import com.hostflow.dto.request.CreateReservationRequest;
import com.hostflow.dto.response.ApiErrorResponse;
import com.hostflow.dto.response.ReservationLinkResponse;
import com.hostflow.dto.response.ReservationResponse;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API de reservas del propietario autenticado (OWNER).
 *
 * <p>Flujo: crear → onboarding del huésped (24 h) → mark-paid → complete (tras check-out, exige PAID).</p>
 */
@RestController
@RequestMapping("/owners/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(
        name = "Reservas del Propietario",
        description = """
                Gestión de reservas del alojamiento del owner.
                El pago ocurre fuera de HostFlow; mark-paid solo registra la verificación del comprobante.
                Todos los endpoints requieren JWT Bearer con rol OWNER.
                """
)
@Slf4j
public class OwnerReservationController {

    private final ReservationService reservationService;
    private final OwnerRepository ownerRepository;
    private final AccommodationRepository accommodationRepository;

    // =========================================================================
    // CREAR
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Crear reserva y generar enlace de onboarding",
            description = """
                    Crea una reserva PENDING y un enlace de onboarding con vigencia fija de 24 horas.
                    Envía el correo al huésped en modo estricto (si falla el envío, se revierte la creación).
                    Check-in puede ser hoy; check-out debe ser posterior. No incluye alergias ni métodos de pago del huésped.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReservationResponse.class),
                            examples = @ExampleObject(name = "PENDING", value = ReservationApiExamples.RES_PENDING))),
            @ApiResponse(responseCode = "400", description = "Validación o negocio (fechas, solapamiento, correo)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "Fechas ocupadas", value = ReservationApiExamples.ERR_400_DATES),
                                    @ExampleObject(name = "Validación", value = ReservationApiExamples.ERR_400_VALIDATION)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "403", description = "Sin rol OWNER",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_403))),
            @ApiResponse(responseCode = "404", description = "Owner o alojamiento no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_ACCOMMODATION)))
    })
    public ResponseEntity<ReservationResponse> createReservation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateReservationRequest.class),
                            examples = @ExampleObject(name = "Crear", value = ReservationApiExamples.REQ_CREATE)
                    )
            )
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        ReservationResponse response = reservationService.createReservation(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // LISTAR / DETALLE
    // =========================================================================

    @GetMapping
    @Operation(summary = "Listar mis reservas",
            description = "Todas las reservas del alojamiento del owner autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservationResponse.class)),
                            examples = @ExampleObject(value = ReservationApiExamples.RES_LIST))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "403", description = "Sin permiso",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_403))),
            @ApiResponse(responseCode = "404", description = "Sin alojamiento",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_ACCOMMODATION)))
    })
    public ResponseEntity<List<ReservationResponse>> listMyReservations(Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tienes un alojamiento registrado. Registra los datos de tu alojamiento primero."));
        return ResponseEntity.ok(reservationService.listReservationsByAccommodation(accommodation.getId()));
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Obtener reserva por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.RES_CONFIRMED))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "404", description = "No existe o no es del owner",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_RESERVATION)))
    })
    public ResponseEntity<ReservationResponse> getReservationById(
            @Parameter(description = "ID de la reserva", example = "12") @PathVariable Long reservationId,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(reservationService.getReservationById(ownerId, reservationId));
    }

    // =========================================================================
    // CANCELAR
    // =========================================================================

    @PostMapping("/{reservationId}/cancel")
    @Operation(
            summary = "Cancelar reserva",
            description = """
                    Cancela PENDING o CONFIRMED (también si está PAID). No cancela COMPLETED.
                    HostFlow no procesa reembolsos. Notifica al huésped tras el commit.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelada",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.RES_CANCELLED))),
            @ApiResponse(responseCode = "400", description = "Ya cancelada o completada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_400_CANCEL))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "404", description = "No encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_RESERVATION)))
    })
    public ResponseEntity<ReservationResponse> cancelReservation(
            @Parameter(description = "ID de la reserva", example = "12") @PathVariable Long reservationId,
            @Parameter(description = "Motivo", example = "Huésped solicitó cancelación")
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(reservationService.cancelReservation(ownerId, reservationId, reason));
    }

    // =========================================================================
    // RENOVAR ENLACE
    // =========================================================================

    @PostMapping("/{reservationId}/reactivate-link")
    @Operation(
            summary = "Renovar enlace de onboarding",
            description = "Solo PENDING. Nuevo enlace 24 h. Correo estricto: fallo de envío revierte la renovación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nuevo enlace de onboarding (24 h)",
                    content = @Content(schema = @Schema(implementation = ReservationLinkResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.RES_LINK))),
            @ApiResponse(responseCode = "400", description = "No PENDING o fallo de correo",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_400_REACTIVATE))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "404", description = "No encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_RESERVATION)))
    })
    public ResponseEntity<ReservationLinkResponse> reactivateLink(
            @Parameter(description = "ID de la reserva", example = "12") @PathVariable Long reservationId,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(reservationService.reactivateLink(ownerId, reservationId));
    }

    // =========================================================================
    // MARK PAID
    // =========================================================================

    @PostMapping("/{reservationId}/mark-paid")
    @Operation(
            summary = "Registrar pago recibido",
            description = """
                    Confirma que el owner recibió el pago fuera de HostFlow.
                    Requiere CONFIRMED con onboarding. Idempotente si ya está PAID.
                    Correos de confirmación en afterCommit (no revierten el pago).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pago registrado",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.RES_PAID))),
            @ApiResponse(responseCode = "400", description = "No CONFIRMED o sin onboarding",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_400_MARK_PAID))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "404", description = "No encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_RESERVATION)))
    })
    public ResponseEntity<ReservationResponse> markPaymentAsPaid(
            @Parameter(description = "ID de la reserva", example = "12") @PathVariable Long reservationId,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(reservationService.markPaymentAsPaid(ownerId, reservationId));
    }

    // =========================================================================
    // COMPLETE
    // =========================================================================

    @PostMapping("/{reservationId}/complete")
    @Operation(
            summary = "Completar reserva (ingreso en dashboard)",
            description = """
                    Requisitos: status CONFIRMED, paymentStatus PAID, y día actual posterior al check-out.
                    Alimenta incomeLastMonth / accumulatedIncome del dashboard.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completada",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.RES_COMPLETED))),
            @ApiResponse(responseCode = "400", description = "Estado, pago o fecha inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "Sin PAID", value = ReservationApiExamples.ERR_400_COMPLETE_PAID),
                                    @ExampleObject(name = "Check-out no pasado", value = ReservationApiExamples.ERR_400_COMPLETE_DATE)
                            })),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "404", description = "No encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_RESERVATION)))
    })
    public ResponseEntity<ReservationResponse> completeReservation(
            @Parameter(description = "ID de la reserva", example = "12") @PathVariable Long reservationId,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(reservationService.completeReservation(ownerId, reservationId));
    }

    // =========================================================================
    // DISPONIBILIDAD
    // =========================================================================

    @GetMapping("/check-availability")
    @Operation(
            summary = "Verificar disponibilidad de fechas",
            description = "true si el rango [checkIn, checkOut) está libre. El día de check-out no bloquea otra reserva."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad",
                    content = @Content(schema = @Schema(implementation = Boolean.class),
                            examples = {
                                    @ExampleObject(name = "Libre", value = "true"),
                                    @ExampleObject(name = "Ocupado", value = "false")
                            })),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_400_DATES_INVALID))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_401))),
            @ApiResponse(responseCode = "404", description = "Sin alojamiento",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = ReservationApiExamples.ERR_404_ACCOMMODATION)))
    })
    public ResponseEntity<Boolean> checkAvailability(
            @Parameter(description = "Check-in YYYY-MM-DD", example = "2026-12-20", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @Parameter(description = "Check-out YYYY-MM-DD", example = "2026-12-23", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            Authentication authentication) {
        Long ownerId = getCurrentOwnerId(authentication);
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tienes un alojamiento registrado. Registra los datos de tu alojamiento primero."));
        boolean available = reservationService.isAccommodationAvailable(
                accommodation.getId(), checkIn, checkOut);
        return ResponseEntity.ok(available);
    }

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

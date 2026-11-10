package com.hostflow.controller;

import com.hostflow.dto.request.CompleteReservationOnboardingRequest;
import com.hostflow.dto.response.ReservationOnboardingConfirmationResponse;
import com.hostflow.dto.response.ReservationOnboardingDetailsResponse;
import com.hostflow.dto.response.ReservationResponse;
import com.hostflow.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST PÚBLICO para que los huéspedes confirmen sus reservas
 * utilizando el enlace de confirmación recibido por email.
 *
 * <p>Estos endpoints no requieren autenticación, ya que la validación
 * se realiza mediante el token único del enlace.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/public/reservations")
@RequiredArgsConstructor
@Tag(name = "Confirmación de Reservas (Público)",
        description = "Endpoints públicos para que los huéspedes confirmen sus reservas")
@Slf4j
public class PublicReservationController {

    private final ReservationService reservationService;

    /**
     * Obtiene los detalles de una reserva usando el token del enlace.
     */
    @GetMapping("/confirm")
    @Operation(
            summary = "Obtener detalles de reserva para confirmar",
            description = """
            Obtiene los detalles de una reserva pendiente usando el token del enlace.
            El huésped puede ver la información antes de confirmar.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva obtenida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Enlace inválido o expirado"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationOnboardingDetailsResponse> getReservationDetails(
            @RequestParam String token) {
        return ResponseEntity.ok(reservationService.getOnboardingDetails(token));
    }

    /**
     * Confirma una reserva usando el token del enlace.
     */
    @PostMapping("/confirm")
    @Operation(
            summary = "Completar onboarding y confirmar reserva",
            description = """
            Guarda datos personales, documento, alergias, preferencias, servicios adicionales
            y el indicador opcional de solicitud de transfer; luego confirma la reserva en
            una sola transacción. No registra pagos: paymentStatus permanece PENDING.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva confirmada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Enlace inválido, expirado o datos de onboarding no válidos"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationOnboardingConfirmationResponse> completeOnboarding(
            @RequestParam String token,
            @Valid @RequestBody CompleteReservationOnboardingRequest request) {
        ReservationOnboardingConfirmationResponse response = reservationService.completeOnboarding(token, request);
        return ResponseEntity.ok(response);
    }
}
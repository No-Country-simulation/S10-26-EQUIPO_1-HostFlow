package com.hostflow.controller;

import com.hostflow.dto.request.ConfirmReservationRequest;
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
    public ResponseEntity<ReservationResponse> getReservationDetails(
            @RequestParam String token) {

        // Buscar la reserva por el token (implementar en servicio)
        // Por ahora, retornar respuesta básica
        return ResponseEntity.ok(null);
    }

    /**
     * Confirma una reserva usando el token del enlace.
     */
    @PostMapping("/confirm")
    @Operation(
            summary = "Confirmar reserva",
            description = """
            Confirma una reserva pendiente usando el token del enlace y seleccionando
            un método de pago activo del huésped.
            
            <strong>Proceso:</strong>
            1. Valida el token del enlace (vigencia y uso único).
            2. Valida que el método de pago sea activo.
            3. Re-valida disponibilidad del alojamiento.
            4. Cambia el estado a CONFIRMED.
            5. Envía email de confirmación al huésped.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva confirmada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Enlace inválido, expirado o método de pago no válido"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    public ResponseEntity<ReservationResponse> confirmReservation(
            @RequestParam String token,
            @Valid @RequestBody ConfirmReservationRequest request) {

        ReservationResponse response = reservationService.confirmReservation(
                token, request.paymentMethodId());
        return ResponseEntity.ok(response);
    }
}
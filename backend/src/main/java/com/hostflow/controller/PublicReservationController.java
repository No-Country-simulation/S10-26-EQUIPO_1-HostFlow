package com.hostflow.controller;

import com.hostflow.dto.request.CompleteReservationOnboardingRequest;
import com.hostflow.dto.response.ApiErrorResponse;
import com.hostflow.dto.response.ReservationOnboardingConfirmationResponse;
import com.hostflow.dto.response.ReservationOnboardingDetailsResponse;
import com.hostflow.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints públicos de onboarding de reserva (sin JWT).
 * El huésped accede con el token del enlace enviado por correo.
 */
@RestController
@RequestMapping("/public/reservations")
@RequiredArgsConstructor
@Tag(
        name = "Onboarding de Reserva (Público)",
        description = """
                El huésped completa la reserva con el token del enlace (24 h).
                No registra pagos: paymentStatus permanece PENDING hasta mark-paid del owner.
                No requiere autenticación.
                """
)
@Slf4j
public class PublicReservationController {

    private final ReservationService reservationService;

    @GetMapping("/confirm")
    @Operation(
            summary = "Detalle de reserva para onboarding",
            description = """
                    Devuelve resumen de estadía, métodos de pago del alojamiento (informativos),
                    servicios adicionales disponibles y equivalencias USD/EUR si aplica
                    (billetera digital / PayPal). Token debe estar vigente y la reserva PENDING.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle para onboarding",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReservationOnboardingDetailsResponse.class),
                            examples = @ExampleObject(name = "Detalle", value = EX_DETAILS))),
            @ApiResponse(responseCode = "400", description = "Token inválido, usado o expirado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = EX_400_TOKEN))),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = EX_404)))
    })
    public ResponseEntity<ReservationOnboardingDetailsResponse> getReservationDetails(
            @Parameter(
                    description = "Token UUID del enlace de onboarding",
                    required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @RequestParam String token) {
        return ResponseEntity.ok(reservationService.getOnboardingDetails(token));
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "Completar onboarding y confirmar reserva",
            description = """
                    Guarda datos personales, documento, alergias, preferencias, servicios adicionales
                    y el flag boatTransferRequested. Confirma la reserva (CONFIRMED).
                    No registra pago: paymentStatus = PENDING.
                    El token solo puede usarse una vez.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding completado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReservationOnboardingConfirmationResponse.class),
                            examples = @ExampleObject(name = "Confirmada", value = EX_CONFIRM))),
            @ApiResponse(responseCode = "400", description = "Token inválido o body inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "Token usado", value = EX_400_TOKEN),
                                    @ExampleObject(name = "Validación", value = EX_400_VALIDATION)
                            })),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = EX_404)))
    })
    public ResponseEntity<ReservationOnboardingConfirmationResponse> completeOnboarding(
            @Parameter(
                    description = "Token UUID del enlace",
                    required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @RequestParam String token,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CompleteReservationOnboardingRequest.class),
                            examples = @ExampleObject(name = "Onboarding", value = EX_ONBOARDING_BODY)
                    )
            )
            @Valid @RequestBody CompleteReservationOnboardingRequest request) {
        return ResponseEntity.ok(reservationService.completeOnboarding(token, request));
    }

    private static final String EX_ONBOARDING_BODY = """
            {
              "firstName": "Juan Carlos",
              "lastName": "Pérez",
              "documentNumber": "30123456",
              "email": "juan.perez@email.com",
              "phone": "+54 11 5555 5555",
              "allergies": ["Maní", "Lactosa"],
              "preferences": "Prefiero piso alto y check-in tarde",
              "additionalServices": [
                { "serviceId": 1, "quantity": 2 },
                { "serviceId": 3, "quantity": 1 }
              ],
              "boatTransferRequested": true
            }
            """;

    private static final String EX_DETAILS = """
            {
              "confirmationCode": "HF-A1B2C3",
              "accommodationName": "Cabañas del Lago",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "guestCount": 2,
              "pricePerNight": 45000.00,
              "accommodationSubtotal": 135000.00,
              "additionalServicesTotal": 0,
              "totalAmount": 135000.00,
              "currency": "ARS",
              "logoUrl": "https://res.cloudinary.com/demo/image/upload/logo.png",
              "primaryColor": "#1E293B",
              "accentColor": "#0EA5E9",
              "tertiaryColor": "#F8FAFC",
              "backgroundPattern": null,
              "welcomeMessage": "¡Bienvenido a Cabañas del Lago!",
              "paymentMethods": [
                {
                  "id": 1,
                  "type": "DIGITAL_WALLET",
                  "displayName": "Mercado Pago",
                  "currency": "ARS",
                  "instructions": "Enviar comprobante por WhatsApp",
                  "bankName": null,
                  "accountHolder": "María Owner",
                  "accountNumber": null,
                  "cbuCvu": null,
                  "alias": null,
                  "providerName": "Mercado Pago",
                  "userIdentifier": "owner.mp",
                  "walletPhone": "+54 11 4444 4444",
                  "walletEmail": "owner@email.com"
                }
              ],
              "additionalServices": [
                {
                  "id": 1,
                  "name": "Desayuno",
                  "description": "Desayuno continental",
                  "basePrice": 5000.00,
                  "priceType": "PER_PERSON",
                  "currency": "ARS"
                }
              ],
              "currencyEquivalents": [
                {
                  "currency": "USD",
                  "pricePerNight": 45.00,
                  "accommodationSubtotal": 135.00,
                  "additionalServicesTotal": 0,
                  "totalAmount": 135.00
                },
                {
                  "currency": "EUR",
                  "pricePerNight": 42.00,
                  "accommodationSubtotal": 126.00,
                  "additionalServicesTotal": 0,
                  "totalAmount": 126.00
                }
              ]
            }
            """;

    private static final String EX_CONFIRM = """
            {
              "reservationId": 12,
              "confirmationCode": "HF-A1B2C3",
              "accommodationId": 1,
              "accommodationName": "Cabañas del Lago",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "guestCount": 2,
              "pricePerNight": 45000.00,
              "accommodationSubtotal": 135000.00,
              "additionalServicesTotal": 15000.00,
              "totalAmount": 150000.00,
              "currency": "ARS",
              "reservationStatus": "CONFIRMED",
              "paymentStatus": "PENDING",
              "boatTransferRequested": true,
              "selectedAdditionalServices": [
                {
                  "serviceId": 1,
                  "name": "Desayuno",
                  "quantity": 2,
                  "unitPrice": 5000.00,
                  "priceType": "PER_PERSON",
                  "nightsApplied": null,
                  "lineTotal": 10000.00,
                  "currency": "ARS"
                }
              ],
              "paymentMethods": [
                {
                  "id": 1,
                  "type": "DIGITAL_WALLET",
                  "displayName": "Mercado Pago",
                  "currency": "ARS",
                  "instructions": "Enviar comprobante por WhatsApp",
                  "bankName": null,
                  "accountHolder": "María Owner",
                  "accountNumber": null,
                  "cbuCvu": null,
                  "alias": null,
                  "providerName": "Mercado",
                  "userIdentifier": "owner.mp",
                  "walletPhone": "+54 11 4444 4444",
                  "walletEmail": "owner@email.com"
                }
              ],
              "currencyEquivalents": [
                {
                  "currency": "USD",
                  "pricePerNight": 45.00,
                  "accommodationSubtotal": 135.00,
                  "additionalServicesTotal": 15.00,
                  "totalAmount": 150.00
                }
              ]
            }
            """;

    private static final String EX_400_TOKEN = """
            {
              "timestamp": "2026-11-10T20:00:00",
              "status": 400,
              "error": "Bad Request",
              "message": "El enlace de reserva es inválido, ya fue utilizado o ha expirado.",
              "errorCode": "BUSINESS_ERROR"
            }
            """;

    private static final String EX_400_VALIDATION = """
            {
              "timestamp": "2026-11-10T20:00:00",
              "status": 400,
              "error": "Bad Request",
              "message": "Error de validación en los datos enviados",
              "errorCode": "VALIDATION_ERROR"
            }
            """;

    private static final String EX_404 = """
            {
              "timestamp": "2026-11-10T20:00:00",
              "status": 404,
              "error": "Not Found",
              "message": "No se encontró la reserva asociada al enlace.",
              "errorCode": "RESOURCE_NOT_FOUND"
            }
            """;
}

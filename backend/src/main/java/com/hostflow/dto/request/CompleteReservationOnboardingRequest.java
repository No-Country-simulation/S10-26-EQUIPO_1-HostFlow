package com.hostflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
        description = "Datos que el huésped completa desde el enlace de onboarding. No registra pago.",
        example = """
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
                """
)
public record CompleteReservationOnboardingRequest(
        @NotBlank @Size(min = 2, max = 100)
        @Schema(description = "Nombre", example = "Juan Carlos")
        String firstName,

        @NotBlank @Size(min = 2, max = 100)
        @Schema(description = "Apellido", example = "Pérez")
        String lastName,

        @NotBlank @Size(min = 5, max = 40)
        @Schema(description = "Documento de identidad", example = "30123456")
        String documentNumber,

        @NotBlank @Email @Size(max = 150)
        @Schema(description = "Correo de contacto", example = "juan.perez@email.com")
        String email,

        @NotBlank @Size(max = 40)
        @Pattern(regexp = "^[+()0-9 .-]{7,40}$", message = "El teléfono debe tener un formato válido")
        @Schema(description = "Teléfono", example = "+54 11 5555 5555")
        String phone,

        @Size(max = 30, message = "No se pueden registrar más de 30 alergias")
        @Schema(description = "Alergias o restricciones alimentarias")
        List<@NotBlank @Size(max = 100) String> allergies,

        @Size(max = 2000)
        @Schema(description = "Preferencias del huésped", example = "Prefiero piso alto")
        String preferences,

        @Valid @Size(max = 30)
        @Schema(description = "Servicios adicionales elegidos (IDs del catálogo del alojamiento)")
        List<AdditionalServiceSelection> additionalServices,

        @Schema(description = "Indica solicitud de transfer en barco/remis; no almacena itinerarios", example = "true")
        boolean boatTransferRequested
) {
    @Schema(description = "Servicio adicional seleccionado y cantidad")
    public record AdditionalServiceSelection(
            @NotNull @Positive
            @Schema(description = "ID del servicio adicional del alojamiento", example = "1")
            Long serviceId,

            @NotNull @Min(1) @Max(100)
            @Schema(description = "Cantidad", example = "2")
            Integer quantity
    ) {}
}

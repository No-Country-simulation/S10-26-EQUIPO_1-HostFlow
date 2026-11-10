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

@Schema(description = "Datos que el huésped completa desde el enlace de onboarding")
public record CompleteReservationOnboardingRequest(
        @NotBlank @Size(min = 2, max = 100) String firstName,
        @NotBlank @Size(min = 2, max = 100) String lastName,
        @NotBlank @Size(min = 5, max = 40) String documentNumber,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[+()0-9 .-]{7,40}$", message = "El teléfono debe tener un formato válido") String phone,
        @Size(max = 30, message = "No se pueden registrar más de 30 alergias") List<@NotBlank @Size(max = 100) String> allergies,
        @Size(max = 2000) String preferences,
        @Valid @Size(max = 30) List<AdditionalServiceSelection> additionalServices,
        @Schema(description = "Solo indica que se solicitó transfer en barco/remis; no se almacenan itinerarios")
        boolean boatTransferRequested
) {
    @Schema(description = "Servicio adicional seleccionado y cantidad")
    public record AdditionalServiceSelection(
            @NotNull @Positive Long serviceId,
            @NotNull @Min(1) @Max(100) Integer quantity
    ) {}
}

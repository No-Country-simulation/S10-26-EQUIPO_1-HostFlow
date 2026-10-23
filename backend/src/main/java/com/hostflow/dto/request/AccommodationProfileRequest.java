package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
import java.math.BigDecimal;

@Schema(name = "AccommodationProfileRequest", description = "Datos generales y de configuración del alojamiento. Los campos distintos de name pueden omitirse cuando no se disponga del dato.")
public record AccommodationProfileRequest(
        @Schema(description = "Nombre comercial del alojamiento.", example = "Cabañas del Río", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String name,

        @Schema(description = "Cantidad de habitaciones.", example = "8", minimum = "1", maximum = "10000", nullable = true)
        @Min(1) @Max(10000) Integer roomCount,

        @Schema(description = "Cantidad de baños.", example = "4", minimum = "0", maximum = "10000", nullable = true)
        @Min(0) @Max(10000) Integer bathroomCount,

        @Schema(description = "Capacidad máxima de huéspedes.", example = "20", minimum = "1", maximum = "100000", nullable = true)
        @Min(1) @Max(100000) Integer guestCapacity,

        @Schema(description = "Precio base del alojamiento. Se admite un máximo de dos decimales y no puede ser negativo.", example = "85000.00", minimum = "0", nullable = true)
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal basePrice,

        @Schema(description = "URL HTTPS del logotipo.", example = "https://example.com/logo.png", maxLength = 2048, nullable = true)
        @URL(protocol = "https", message = "logoUrl debe ser una URL HTTPS válida") @Size(max = 2048) String logoUrl,

        @Schema(description = "Color principal en formato hexadecimal de 3 o 6 dígitos.", example = "#1E293B", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", nullable = true)
        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "primaryColor debe ser un color hexadecimal, por ejemplo #AABBCC") String primaryColor,

        @Schema(description = "Mensaje de bienvenida del alojamiento.", example = "¡Te damos la bienvenida!", maxLength = 5000, nullable = true)
        @Size(max = 5000) String welcomeMessage,

        @Schema(description = "Correo electrónico de contacto.", example = "reservas@example.com", maxLength = 150, nullable = true)
        @Email @Size(max = 150) String email,

        @Schema(description = "Teléfono de contacto.", example = "+54 11 5555 5555", maxLength = 40, nullable = true)
        @Size(max = 40) String phone,

        @Schema(description = "Dirección del alojamiento.", example = "Delta del Tigre", maxLength = 5000, nullable = true)
        @Size(max = 5000) String address,

        @Schema(description = "Nombre del banco o billetera.", example = "Banco de ejemplo", maxLength = 150, nullable = true)
        @Size(max = 150) String bankName,

        @Schema(description = "Titular de la cuenta bancaria.", example = "Alojamiento Ejemplo", maxLength = 200, nullable = true)
        @Size(max = 200) String bankAccountHolder,

        @Schema(description = "Número de cuenta bancaria, cuando corresponda.", example = "123456789", maxLength = 100, nullable = true)
        @Size(max = 100) String bankAccountNumber,

        @Schema(description = "CBU o CVU para transferencias.", example = "0000000000000000000000", maxLength = 22, nullable = true)
        @Size(max = 22) String bankCbuCvu,

        @Schema(description = "Alias bancario para transferencias.", example = "CABANAS.RIO", maxLength = 100, nullable = true)
        @Size(max = 100) String bankAlias,

        @Schema(description = "Identificación fiscal del titular.", example = "20123456789", maxLength = 20, nullable = true)
        @Size(max = 20) String bankCuit
) {}

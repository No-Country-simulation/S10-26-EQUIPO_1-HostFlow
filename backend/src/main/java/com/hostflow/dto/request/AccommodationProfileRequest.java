package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(name = "AccommodationProfileRequest", description = "Datos generales y de configuración del alojamiento. Para PATCH, los campos omitidos o null no se modifican.")
public class AccommodationProfileRequest {
    @Schema(description = "Nombre comercial del alojamiento.", example = "Cabañas del Río", maxLength = 200, nullable = true)
    @Size(max = 200)
    private String name;

    @Schema(description = "Cantidad de habitaciones.", example = "8", minimum = "1", maximum = "10000", nullable = true)
    @Min(1) @Max(10000)
    private Integer roomCount;

    @Schema(description = "Cantidad de baños.", example = "4", minimum = "0", maximum = "10000", nullable = true)
    @Min(0) @Max(10000)
    private Integer bathroomCount;

    @Schema(description = "Capacidad máxima de huéspedes.", example = "20", minimum = "1", maximum = "100000", nullable = true)
    @Min(1) @Max(100000)
    private Integer guestCapacity;

    @Schema(description = "Precio base del alojamiento. Se admite un máximo de dos decimales y no puede ser negativo.", example = "85000.00", minimum = "0", nullable = true)
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    private BigDecimal basePrice;

    @Schema(description = "Color principal en formato hexadecimal de 3 o 6 dígitos.", example = "#1E293B", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", nullable = true)
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "primaryColor debe ser un color hexadecimal, por ejemplo #AABBCC")
    private String primaryColor;

    @Schema(description = "Color de acento en formato hexadecimal de 3 o 6 dígitos.", example = "#d4f53c", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", nullable = true)
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "accentColor debe ser un color hexadecimal, por ejemplo #D4F53C")
    private String accentColor;

    @Schema(description = "Color terciario en formato hexadecimal de 3 o 6 dígitos.", example = "#f4b8c4", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", nullable = true)
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "tertiaryColor debe ser un color hexadecimal, por ejemplo #F4B8C4")
    private String tertiaryColor;

    @Schema(description = "Identificador del patrón de fondo. Valores permitidos: none, leaves, waves, petals, arches, mountains, scales, sun.", example = "none", allowableValues = {"none", "leaves", "waves", "petals", "arches", "mountains", "scales", "sun"}, nullable = true)
    @Pattern(regexp = "^(none|leaves|waves|petals|arches|mountains|scales|sun)$", message = "backgroundPattern debe ser uno de los identificadores permitidos")
    private String backgroundPattern;

    @Schema(description = "Mensaje de bienvenida del alojamiento.", example = "¡Te damos la bienvenida!", maxLength = 5000, nullable = true)
    @Size(max = 5000)
    private String welcomeMessage;

    @Schema(description = "Correo electrónico de contacto.", example = "reservas@example.com", maxLength = 150, nullable = true)
    @Email @Size(max = 150)
    private String email;

    @Schema(description = "Teléfono de contacto.", example = "+54 11 5555 5555", maxLength = 40, nullable = true)
    @Size(max = 40)
    private String phone;

    @Schema(description = "Dirección del alojamiento.", example = "Delta del Tigre", maxLength = 5000, nullable = true)
    @Size(max = 5000)
    private String address;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getRoomCount() { return roomCount; }
    public void setRoomCount(Integer roomCount) { this.roomCount = roomCount; }
    public Integer getBathroomCount() { return bathroomCount; }
    public void setBathroomCount(Integer bathroomCount) { this.bathroomCount = bathroomCount; }
    public Integer getGuestCapacity() { return guestCapacity; }
    public void setGuestCapacity(Integer guestCapacity) { this.guestCapacity = guestCapacity; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }
    public String getTertiaryColor() { return tertiaryColor; }
    public void setTertiaryColor(String tertiaryColor) { this.tertiaryColor = tertiaryColor; }
    public String getBackgroundPattern() { return backgroundPattern; }
    public void setBackgroundPattern(String backgroundPattern) { this.backgroundPattern = backgroundPattern; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}

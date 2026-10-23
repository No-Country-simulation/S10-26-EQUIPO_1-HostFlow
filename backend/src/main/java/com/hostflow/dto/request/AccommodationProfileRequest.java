package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
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

    @Schema(description = "URL HTTPS del logotipo.", example = "https://example.com/logo.png", maxLength = 2048, nullable = true)
    @URL(protocol = "https", message = "logoUrl debe ser una URL HTTPS válida") @Size(max = 2048)
    private String logoUrl;

    @Schema(description = "Color principal en formato hexadecimal de 3 o 6 dígitos.", example = "#1E293B", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", nullable = true)
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "primaryColor debe ser un color hexadecimal, por ejemplo #AABBCC")
    private String primaryColor;

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
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}

package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO para las operaciones de actualización (PATCH) del perfil del alojamiento.
 *
 * <p>Este objeto representa los datos que el propietario puede enviar al modificar
 * su alojamiento. Todos los campos son opcionales (null = no modificar).</p>
 *
 * <p><strong>Comportamiento PATCH:</strong> Solo se actualizan los campos explícitamente
 * enviados. Los campos omitidos o null permanecen sin cambios.</p>
 *
 * <p><strong>Validaciones:</strong></p>
 * <ul>
 *   <li>Campos numéricos con rangos válidos.</li>
 *   <li>Colores en formato hexadecimal (#RGB o #RRGGBB).</li>
 *   <li>Email con formato válido.</li>
 *   <li>Tamaños máximos para strings largos.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Schema(name = "AccommodationProfileRequest",
        description = "Datos generales y de configuración del alojamiento. Para PATCH, los campos omitidos o null no se modifican.")
public class AccommodationProfileRequest {

    // =========================================================================
    // CAMPOS BÁSICOS DEL ALOJAMIENTO
    // =========================================================================

    /**
     * Nombre comercial del alojamiento.
     * Máximo 200 caracteres.
     */
    @Schema(description = "Nombre comercial del alojamiento.", example = "Cabañas del Río", maxLength = 200)
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String name;

    /**
     * Cantidad total de habitaciones.
     * Mínimo 1, máximo 10000.
     */
    @Schema(description = "Cantidad de habitaciones.", example = "8", minimum = "1", maximum = "10000")
    @Min(value = 1, message = "La cantidad de habitaciones debe ser al menos 1")
    @Max(value = 10000, message = "La cantidad de habitaciones no puede exceder 10000")
    private Integer roomCount;

    /**
     * Cantidad total de baños.
     * Mínimo 0, máximo 10000.
     */
    @Schema(description = "Cantidad de baños.", example = "4", minimum = "0", maximum = "10000")
    @Min(value = 0, message = "La cantidad de baños no puede ser negativa")
    @Max(value = 10000, message = "La cantidad de baños no puede exceder 10000")
    private Integer bathroomCount;

    /**
     * Capacidad máxima de huéspedes.
     * Mínimo 1, máximo 100000.
     */
    @Schema(description = "Capacidad máxima de huéspedes.", example = "20", minimum = "1", maximum = "100000")
    @Min(value = 1, message = "La capacidad debe ser al menos 1 huésped")
    @Max(value = 100000, message = "La capacidad no puede exceder 100000 huéspedes")
    private Integer guestCapacity;

    /**
     * Precio base por noche.
     * No negativo, máximo 2 decimales.
     */
    @Schema(description = "Precio base del alojamiento. Se admite un máximo de dos decimales y no puede ser negativo.",
            example = "85000.00", minimum = "0")
    @DecimalMin(value = "0.00", message = "El precio base no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio base debe tener máximo 2 decimales")
    private BigDecimal basePrice;

    // =========================================================================
    // IDENTIDAD VISUAL Y BRANDING
    // =========================================================================

    /**
     * URL del logotipo del alojamiento.
     * Máximo 2048 caracteres (URL válida).
     */
    @Schema(description = "URL HTTPS del logotipo del alojamiento.",
            example = "https://example.com/logo.png", maxLength = 2048)
    @Size(max = 2048, message = "La URL del logo no puede exceder 2048 caracteres")
    private String logoUrl;

    /**
     * Color principal en formato hexadecimal.
     * Patrón: #RGB o #RRGGBB.
     */
    @Schema(description = "Color principal en formato hexadecimal de 3 o 6 dígitos.",
            example = "#0fa3a3", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "El color principal debe ser un hexadecimal válido (ej: #AABBCC o #ABC)")
    @Size(max = 20, message = "El color principal no puede exceder 20 caracteres")
    private String primaryColor;

    /**
     * Color de acento en formato hexadecimal.
     * Patrón: #RGB o #RRGGBB.
     */
    @Schema(description = "Color de acento en formato hexadecimal de 3 o 6 dígitos.",
            example = "#d4f53c", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "El color de acento debe ser un hexadecimal válido (ej: #AABBCC o #ABC)")
    @Size(max = 7, message = "El color de acento no puede exceder 7 caracteres")
    private String accentColor;

    /**
     * Color terciario en formato hexadecimal.
     * Patrón: #RGB o #RRGGBB.
     */
    @Schema(description = "Color terciario en formato hexadecimal de 3 o 6 dígitos.",
            example = "#f4b8c4", pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "El color terciario debe ser un hexadecimal válido (ej: #AABBCC o #ABC)")
    @Size(max = 7, message = "El color terciario no puede exceder 7 caracteres")
    private String tertiaryColor;

    /**
     * Identificador del patrón de fondo.
     * Máximo 20 caracteres.
     */
    @Schema(description = "Identificador del patrón de fondo.", example = "none", maxLength = 20)
    @Size(max = 20, message = "El patrón de fondo no puede exceder 20 caracteres")
    private String backgroundPattern;

    // =========================================================================
    // INFORMACIÓN DE CONTACTO Y COMUNICACIÓN
    // =========================================================================

    /**
     * Mensaje de bienvenida personalizado.
     * Máximo 5000 caracteres.
     */
    @Schema(description = "Mensaje de bienvenida del alojamiento.",
            example = "¡Te damos la bienvenida!", maxLength = 5000)
    @Size(max = 5000, message = "El mensaje de bienvenida no puede exceder 5000 caracteres")
    private String welcomeMessage;

    /**
     * Correo electrónico de contacto.
     * Formato email válido, máximo 150 caracteres.
     */
    @Schema(description = "Correo electrónico de contacto.",
            example = "reservas@example.com", maxLength = 150)
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 150, message = "El correo electrónico no puede exceder 150 caracteres")
    private String email;

    /**
     * Teléfono de contacto.
     * Máximo 40 caracteres.
     */
    @Schema(description = "Teléfono de contacto.", example = "+54 11 5555 5555", maxLength = 40)
    @Size(max = 40, message = "El teléfono no puede exceder 40 caracteres")
    private String phone;

    /**
     * Dirección física del alojamiento.
     * Máximo 5000 caracteres.
     */
    @Schema(description = "Dirección del alojamiento.", example = "Delta del Tigre", maxLength = 5000)
    @Size(max = 5000, message = "La dirección no puede exceder 5000 caracteres")
    private String address;

    // =========================================================================
    // GETTERS Y SETTERS
    // =========================================================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    public Integer getBathroomCount() {
        return bathroomCount;
    }

    public void setBathroomCount(Integer bathroomCount) {
        this.bathroomCount = bathroomCount;
    }

    public Integer getGuestCapacity() {
        return guestCapacity;
    }

    public void setGuestCapacity(Integer guestCapacity) {
        this.guestCapacity = guestCapacity;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getTertiaryColor() {
        return tertiaryColor;
    }

    public void setTertiaryColor(String tertiaryColor) {
        this.tertiaryColor = tertiaryColor;
    }

    public String getBackgroundPattern() {
        return backgroundPattern;
    }

    public void setBackgroundPattern(String backgroundPattern) {
        this.backgroundPattern = backgroundPattern;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
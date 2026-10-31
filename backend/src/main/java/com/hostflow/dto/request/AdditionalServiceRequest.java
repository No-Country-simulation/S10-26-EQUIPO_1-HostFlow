package com.hostflow.dto.request;

import com.hostflow.enums.ServicePriceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para crear o actualizar un servicio adicional del alojamiento.
 *
 * <p>Se utiliza en los endpoints de gestión de servicios adicionales para que el
 * propietario pueda registrar servicios extras que complementan la estadía, tales como:</p>
 * <ul>
 *   <li>Desayuno buffet (generalmente cobrado por día)</li>
 *   <li>Transfer al aeropuerto (generalmente cobro único total)</li>
 *   <li>Spa / masajes</li>
 *   <li>Estacionamiento</li>
 *   <li>Mascotas</li>
 *   <li>Early check-in / Late check-out</li>
 * </ul>
 *
 * <p><strong>🆕 Novedades en la versión 2.0:</strong></p>
 * <p>Se incorporó el campo {@link #priceType} para permitir al propietario especificar
 * si el servicio se cobra por cada día de estadía ({@code PER_DAY}) o como un monto
 * único e independiente de la duración ({@code TOTAL}). Si el cliente omite este campo,
 * el servicio de negocio asignará {@code TOTAL} como valor predeterminado.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see com.hostflow.enums.ServicePriceType
 * @see com.hostflow.service.AdditionalServiceService
 */
@Schema(description = "Datos requeridos para crear o actualizar un servicio adicional del alojamiento")
public record AdditionalServiceRequest(

        /**
         * Nombre comercial o descriptivo del servicio adicional.
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @NotBlank}: No puede ser nulo, vacío ni contener solo espacios.</li>
         *   <li>{@code @Size(max = 150)}: Longitud máxima de 150 caracteres para evitar
         *       desbordamientos visuales en la interfaz de usuario.</li>
         * </ul>
         */
        @NotBlank(message = "El nombre del servicio es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        @Schema(
                description = "Nombre del servicio adicional",
                example = "Desayuno buffet continental",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        /**
         * Descripción detallada y opcional del servicio.
         *
         * <p>Proporciona contexto adicional al huésped sobre qué incluye el servicio,
         * horarios, restricciones o condiciones especiales.</p>
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @Size(max = 1000)}: Longitud máxima de 1000 caracteres.</li>
         *   <li>Es un campo opcional (puede ser {@code null}).</li>
         * </ul>
         */
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Schema(
                description = "Descripción detallada del servicio, horarios o condiciones",
                example = "Desayuno continental servido de 8:00 a 10:00 hs en el comedor principal. Incluye café, jugo, panificados y frutas.",
                maxLength = 1000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String description,

        /**
         * Precio base del servicio en la moneda principal del alojamiento (ARS).
         *
         * <p>Este valor se almacenará en la base de datos y se convertirá automáticamente
         * a las demás monedas habilitadas (USD, EUR) mediante el {@code CurrencyConversionService}
         * al momento de ser consultado por un huésped.</p>
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @NotNull}: El campo es obligatorio.</li>
         *   <li>{@code @DecimalMin("0.01")}: El valor debe ser estrictamente mayor a cero,
         *       evitando la creación de servicios gratuitos no intencionales.</li>
         * </ul>
         */
        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio base debe ser mayor a 0")
        @Schema(
                description = "Precio del servicio en la moneda base del alojamiento (ARS)",
                example = "5000.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal basePrice,

        /**
         * 🆕 Tipo de cobro aplicado a este servicio adicional.
         *
         * <p>Define la lógica de facturación del servicio:</p>
         * <ul>
         *   <li>{@code PER_DAY}: El precio se multiplica por la cantidad de noches de estadía
         *       (ej: Desayuno buffet, Estacionamiento).</li>
         *   <li>{@code TOTAL}: El precio es un monto único e independiente de la duración
         *       de la estadía (ej: Transfer al aeropuerto, Limpieza final).</li>
         * </ul>
         *
         * <p><strong>Nota de negocio:</strong> Este campo es opcional en la petición.
         * Si el cliente lo omite ({@code null}), el servicio de negocio asignará
         * automáticamente {@link ServicePriceType#TOTAL} como valor predeterminado.</p>
         */
        @Schema(
                description = "Tipo de cobro: PER_DAY (por cada día de estadía) o TOTAL (monto único). " +
                        "Si se omite, se asume TOTAL por defecto.",
                example = "PER_DAY",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        ServicePriceType priceType,

        /**
         * Estado de visibilidad del servicio.
         *
         * <p>Indica si el servicio está activo y debe ser mostrado a los huéspedes
         * durante el proceso de reserva. Un servicio inactivo ({@code false})
         * permanece en la base de datos pero no se ofrece.</p>
         *
         * <p><strong>Nota de negocio:</strong> Si se omite en la petición, el servicio
         * de negocio lo interpretará como {@code true} (activo por defecto).</p>
         */
        @Schema(
                description = "Indica si el servicio está activo y visible para los huéspedes",
                example = "true",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Boolean active

) {}
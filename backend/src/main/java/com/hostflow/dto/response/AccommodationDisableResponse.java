package com.hostflow.dto.response;

import com.hostflow.model.AccommodationTemporaryDisable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) para la respuesta de desactivación temporal de alojamiento.
 *
 * <p>Expone únicamente los campos necesarios para la API REST, ocultando
 * la estructura interna de la entidad JPA {@link AccommodationTemporaryDisable}
 * y sus métodos de dominio ({@code isActiveOnDate}, {@code getDurationInDays}, {@code isExpired}).</p>
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.0):</strong></p>
 * <ul>
 *   <li>Incluye el método factory {@link #from(AccommodationTemporaryDisable)} que
 *       centraliza la conversión desde la entidad JPA, evitando duplicación
 *       de lógica de mapeo en controladores y servicios (Principio DRY).</li>
 *   <li>Maneja de forma segura el caso {@code null}, indispensable porque
 *       el endpoint {@code GET /status} retorna {@code null} cuando no existe
 *       una desactivación activa para el alojamiento.</li>
 *   <li>Incluye anotaciones {@link Schema} de OpenAPI 3 para que Swagger UI
 *       documente automáticamente cada campo con descripciones y ejemplos.</li>
 * </ul>
 *
 * <p><strong>Patrón de diseño:</strong> Se utilizan las anotaciones de Lombok
 * {@link Data}, {@link Builder}, {@link NoArgsConstructor} y {@link AllArgsConstructor}
 * para reducir el código boilerplate manteniendo la funcionalidad completa
 * (getters, setters, toString, equals, hashCode y patrón Builder).</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see AccommodationTemporaryDisable
 * @see com.hostflow.service.AccommodationTemporaryDisableService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "AccommodationDisableResponse",
        description = "Información de una desactivación temporal del alojamiento, "
                + "incluyendo motivo, rango de fechas y estado actual."
)
public class AccommodationDisableResponse {

    // =========================================================================
    // CAMPOS DEL DTO
    // =========================================================================

    /**
     * Identificador único del registro de desactivación temporal.
     *
     * <p>Se utiliza para referencia en operaciones de auditoría
     * y en la visualización del historial de desactivaciones.</p>
     */
    @Schema(
            description = "Identificador único del registro de desactivación",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    /**
     * Identificador del propietario que registró la desactivación.
     *
     * <p>Referencia lógica a la entidad {@code Owner}. Se expone
     * para facilitar la correlación de datos en el frontend.</p>
     */
    @Schema(
            description = "ID del propietario que registró la desactivación",
            example = "15",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long ownerId;

    /**
     * Motivo o razón descriptiva de la desactivación temporal.
     *
     * <p>Ejemplos típicos: "Remodelación", "Mantenimiento preventivo",
     * "Vacaciones del propietario", "Cierre por temporada".</p>
     */
    @Schema(
            description = "Motivo descriptivo de la desactivación temporal",
            example = "Mantenimiento preventivo anual de sistemas eléctricos",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 500
    )
    private String reason;

    /**
     * Fecha y hora de inicio de la desactivación.
     *
     * <p>A partir de este momento el alojamiento deja de estar
     * disponible para nuevas reservas.</p>
     */
    @Schema(
            description = "Fecha y hora de inicio de la desactivación",
            example = "2026-12-01T00:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime disabledFrom;

    /**
     * Fecha y hora estimada de reactivación del alojamiento.
     *
     * <p>Puede ser {@code null} cuando la duración de la desactivación
     * es indeterminada (por ejemplo, reparaciones de emergencia donde
     * no se puede estimar el tiempo de finalización).</p>
     */
    @Schema(
            description = "Fecha estimada de reactivación (null si es indeterminada)",
            example = "2026-12-10T23:59:59",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private LocalDateTime disabledUntil;

    /**
     * Indica si la desactivación está actualmente vigente.
     *
     * <ul>
     *   <li>{@code true}: desactivación activa, el alojamiento no acepta reservas.</li>
     *   <li>{@code false}: desactivación histórica (ya reactivada manualmente
     *       o finalizada automáticamente por cumplimiento de {@code disabledUntil}).</li>
     * </ul>
     */
    @Schema(
            description = "Indica si la desactivación está actualmente vigente",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean isActive;

    /**
     * Fecha y hora en que se registró la desactivación.
     *
     * <p>Útil para ordenar el historial de desactivaciones
     * y para fines de auditoría.</p>
     */
    @Schema(
            description = "Fecha y hora en que se registró la desactivación",
            example = "2026-11-15T10:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime createdAt;

    // =========================================================================
    // MÉTODO FACTORY - MAPEO DESDE ENTIDAD JPA
    // =========================================================================

    /**
     * Convierte una entidad JPA {@link AccommodationTemporaryDisable} en este DTO.
     *
     * <p><strong>🛡️ Requisito indispensable:</strong> Este método maneja de forma
     * segura el caso en que la entidad sea {@code null}, retornando {@code null}
     * en lugar de lanzar {@link NullPointerException}. Esto es crítico porque
     * el endpoint {@code GET /owners/accommodation/status} utiliza este DTO
     * para representar el estado actual del alojamiento, y cuando NO existe
     * una desactivación activa, el servicio retorna {@code null}.</p>
     *
     * <p><strong>Ventajas de centralizar el mapeo aquí:</strong></p>
     * <ul>
     *   <li><strong>DRY (Don't Repeat Yourself):</strong> Evita duplicar la lógica
     *       de mapeo campo por campo en múltiples controladores y servicios.</li>
     *   <li><strong>Encapsulamiento:</strong> Solo este DTO conoce su propia estructura
     *       y cómo se construye a partir de la entidad JPA.</li>
     *   <li><strong>Mantenibilidad:</strong> Si se agrega o elimina un campo en el futuro,
     *       solo se modifica este método, no todas las capas de la aplicación.</li>
     *   <li><strong>Seguridad:</strong> Garantiza que NUNCA se exponga la entidad JPA
     *       directamente en la API, previniendo fugas de datos internos.</li>
     * </ul>
     *
     * @param entity la entidad JPA {@link AccommodationTemporaryDisable} a convertir.
     *               Puede ser {@code null} (por ejemplo, cuando no hay desactivación activa).
     * @return el DTO {@link AccommodationDisableResponse} con los datos mapeados,
     *         o {@code null} si la entidad de entrada es {@code null}.
     *
     * @see com.hostflow.service.AccommodationTemporaryDisableService#getActiveDisableByEmail(String)
     */
    public static AccommodationDisableResponse from(AccommodationTemporaryDisable entity) {
        // 🛡️ Manejo seguro de null: indispensable para GET /status cuando no hay desactivación activa
        if (entity == null) {
            return null;
        }

        // Construcción del DTO usando el patrón Builder de Lombok
        return AccommodationDisableResponse.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .reason(entity.getReason())
                .disabledFrom(entity.getDisabledFrom())
                .disabledUntil(entity.getDisabledUntil())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
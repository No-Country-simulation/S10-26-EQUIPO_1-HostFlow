package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa la desactivación temporal de un alojamiento
 * gestionado por su propietario en el sistema HostFlow.
 *
 * <p>Esta entidad permite a los dueños de alojamientos marcar temporalmente
 * su propiedad como no disponible por diversos motivos operativos, sin afectar
 * el estado general de su cuenta en la plataforma.</p>
 *
 * <p><strong>Casos de uso principales:</strong></p>
 * <ul>
 *   <li><strong>Remodelaciones:</strong> Cuando el alojamiento requiere mejoras
 *       estructurales o de equipamiento que impiden su operación normal.</li>
 *   <li><strong>Mantenimiento preventivo:</strong> Revisiones técnicas programadas
 *       de instalaciones, equipos o sistemas.</li>
 *   <li><strong>Vacaciones del propietario:</strong> Períodos donde el dueño no
 *       puede atender el alojamiento personalmente.</li>
 *   <li><strong>Cierre por temporada:</strong> Suspensión de operaciones durante
 *       períodos de baja demanda o condiciones climáticas adversas.</li>
 *   <li><strong>Emergencias:</strong> Situaciones imprevistas que requieren cierre
 *       inmediato (daños, desastres naturales, etc.).</li>
 * </ul>
 *
 * <p><strong>Características clave:</strong></p>
 * <ul>
 *   <li><strong>Independencia del estado de cuenta:</strong> La desactivación del
 *       alojamiento no afecta el campo {@code enabled} del {@link Owner}. El
 *       propietario mantiene acceso completo al sistema.</li>
 *   <li><strong>Rango temporal flexible:</strong> Permite especificar fecha de
 *       inicio obligatoria y fecha de fin opcional (para casos de duración
 *       indeterminada).</li>
 *   <li><strong>Historial completo:</strong> Mantiene registro de todas las
 *       desactivaciones realizadas, incluyendo las ya finalizadas.</li>
 *   <li><strong>Control de estado:</strong> El campo {@code isActive} permite
 *       marcar desactivaciones como completadas sin eliminar el registro.</li>
 * </ul>
 *
 * <p><strong>Impacto en el sistema:</strong></p>
 * <ul>
 *   <li>El alojamiento no aparecerá disponible para nuevas reservas durante
 *       el período de desactivación.</li>
 *   <li>Las reservas existentes pueden requerir notificación al huésped
 *       (lógica a implementar en servicio de reservas).</li>
 *   <li>El propietario puede reactivar el alojamiento antes de la fecha
 *       estimada si las condiciones lo permiten.</li>
 * </ul>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>Un propietario solo puede tener UNA desactivación activa a la vez.</li>
 *   <li>La fecha de inicio no puede ser en el pasado.</li>
 *   <li>La fecha de fin (si existe) debe ser posterior a la fecha de inicio.</li>
 *   <li>Solo el propietario del alojamiento puede crear/modificar su desactivación.</li>
 * </ul>
 *
 * <p><strong>Relaciones:</strong></p>
 * <ul>
 *   <li>Relación ManyToOne implícita con {@link Owner} a través del campo
 *       {@code ownerId} (no se usa relación JPA directa para mantener
 *       independencia y permitir consultas eficientes).</li>
 * </ul>
 *
 * <p><strong>Índices de base de datos:</strong></p>
 * <ul>
 *   <li>{@code idx_owner_active}: Optimiza consultas para verificar si un
 *       propietario tiene desactivación activa.</li>
 *   <li>{@code idx_disable_dates}: Optimiza consultas de disponibilidad
 *       en rangos de fechas.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see com.hostflow.service.AccommodationTemporaryDisableService
 * @see com.hostflow.repository.AccommodationTemporaryDisableRepository
 * @see Owner
 */
@Entity
@Table(
        name = "accommodation_temporary_disables",
        indexes = {
                /**
                 * Índice compuesto para consultas frecuentes de estado activo por propietario.
                 * Optimiza: findByOwnerIdAndIsActiveTrue()
                 */
                @Index(
                        name = "idx_owner_active",
                        columnList = "owner_id, is_active"
                ),
                /**
                 * Índice para consultas de disponibilidad en rangos de fechas.
                 * Optimiza: findActiveDisableAtDate()
                 */
                @Index(
                        name = "idx_disable_dates",
                        columnList = "disabled_from, disabled_until"
                ),
                /**
                 * Índice para consultas de historial ordenado por fecha de creación.
                 * Optimiza: findByOwnerIdOrderByCreatedAtDesc()
                 */
                @Index(
                        name = "idx_owner_created",
                        columnList = "owner_id, created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationTemporaryDisable {

    // =========================================================================
    // IDENTIFICADOR
    // =========================================================================

    /**
     * Identificador único y clave primaria del registro de desactivación temporal.
     *
     * <p>Se utiliza {@link GenerationType#IDENTITY} para delegar la generación
     * del valor al motor de la base de datos (AUTO_INCREMENT en PostgreSQL),
     * lo cual es la práctica más eficiente y portable para claves primarias
     * secuenciales.</p>
     *
     * <p>Ejemplo: {@code 1}, {@code 2}, {@code 3}, ...</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // RELACIÓN CON PROPIETARIO
    // =========================================================================

    /**
     * Identificador del dueño del alojamiento que solicita la desactivación.
     *
     * <p>Este campo establece la relación con la entidad {@link Owner} sin
     * usar una relación JPA directa (@ManyToOne). Esta decisión de diseño
     * tiene las siguientes ventajas:</p>
     * <ul>
     *   <li><strong>Independencia:</strong> Permite consultar desactivaciones
     *       sin cargar la entidad completa del propietario.</li>
     *   <li><strong>Rendimiento:</strong> Evita joins innecesarios en consultas
     *       frecuentes de estado.</li>
     *   <li><strong>Flexibilidad:</strong> Facilita migraciones futuras si se
     *       requiere cambiar la estructura de propietarios.</li>
     * </ul>
     *
     * <p><strong>Restricciones:</strong></p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>Debe corresponder a un ID válido en la tabla {@code owners}</li>
     * </ul>
     *
     * <p>Ejemplo: {@code 15} (ID del propietario Carlos Palacios)</p>
     *
     * @see Owner#getId()
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // =========================================================================
    // INFORMACIÓN DE LA DESACTIVACIÓN
    // =========================================================================

    /**
     * Motivo o razón de la desactivación temporal del alojamiento.
     *
     * <p>Este campo es obligatorio y debe proporcionar información clara
     * sobre por qué el alojamiento no estará disponible. La información
     * puede ser utilizada para:</p>
     * <ul>
     *   <li>Notificar a huéspedes con reservas existentes</li>
     *   <li>Generar reportes administrativos</li>
     *   <li>Mantener historial operativo del alojamiento</li>
     * </ul>
     *
     * <p><strong>Ejemplos de motivos válidos:</strong></p>
     * <ul>
     *   <li>"Remodelación completa de baños y cocina - Mejora de instalaciones"</li>
     *   <li>"Mantenimiento preventivo anual de sistemas eléctricos y plomería"</li>
     *   <li>"Cierre por temporada baja - Noviembre a Marzo"</li>
     *   <li>"Vacaciones del propietario - Viaje familiar programado"</li>
     *   <li>"Daños por tormenta - Reparación de techo y ventanas"</li>
     *   <li>"Actualización de mobiliario y equipamiento"</li>
     * </ul>
     *
     * <p><strong>Restricciones:</strong></p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>Longitud máxima: 500 caracteres</li>
     *   <li>Debe ser descriptivo y profesional</li>
     * </ul>
     */
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    // =========================================================================
    // RANGO TEMPORAL
    // =========================================================================

    /**
     * Fecha y hora de inicio de la desactivación temporal.
     *
     * <p>Representa el momento exacto desde el cual el alojamiento dejará
     * de estar disponible para nuevas reservas. Esta fecha es obligatoria
     * y no puede ser en el pasado al momento de crear el registro.</p>
     *
     * <p><strong>Consideraciones importantes:</strong></p>
     * <ul>
     *   <li>Debe ser igual o posterior a la fecha/hora actual</li>
     *   <li>Puede ser una fecha futura programada (ej. cerrar desde el
     *       próximo mes)</li>
     *   <li>El sistema debe validar que no se superponga con otra
     *       desactivación activa del mismo propietario</li>
     * </ul>
     *
     * <p><strong>Formato:</strong> ISO-8601 (ej: {@code 2026-11-01T00:00:00})</p>
     *
     * <p>Ejemplo: {@code 2026-11-01T00:00:00} (1 de noviembre de 2026, medianoche)</p>
     */
    @Column(name = "disabled_from", nullable = false)
    private LocalDateTime disabledFrom;

    /**
     * Fecha y hora estimada de finalización de la desactivación temporal.
     *
     * <p>Representa el momento en que se prevé que el alojamiento volverá
     * a estar disponible. Este campo es <strong>opcional</strong> para
     * accommodating casos donde la duración es indeterminada.</p>
     *
     * <p><strong>Casos de uso:</strong></p>
     * <ul>
     *   <li><strong>Con fecha definida:</strong> Remodelaciones con cronograma
     *       fijo, cierres por temporada, vacaciones programadas.</li>
     *   <li><strong>Sin fecha definida (null):</strong> Reparaciones de
     *       emergencia, mantenimientos correctivos, situaciones imprevistas
     *       donde no se puede estimar la duración.</li>
     * </ul>
     *
     * <p><strong>Restricciones:</strong></p>
     * <ul>
     *   <li>Puede ser {@code null} (duración indeterminada)</li>
     *   <li>Si se especifica, debe ser posterior a {@code disabledFrom}</li>
     *   <li>El propietario puede modificar esta fecha si las condiciones cambian</li>
     * </ul>
     *
     * <p><strong>Formato:</strong> ISO-8601 (ej: {@code 2026-11-15T23:59:59})</p>
     *
     * <p>Ejemplo: {@code 2026-11-15T23:59:59} (15 de noviembre de 2026, final del día)</p>
     */
    @Column(name = "disabled_until")
    private LocalDateTime disabledUntil;

    // =========================================================================
    // ESTADO DEL REGISTRO
    // =========================================================================

    /**
     * Indicador de si la desactivación temporal está actualmente vigente.
     *
     * <p>Este campo controla el estado activo/inactivo del registro y es
     * fundamental para las consultas de disponibilidad del alojamiento.</p>
     *
     * <p><strong>Ciclo de vida del estado:</strong></p>
     * <ol>
     *   <li><strong>Creación:</strong> {@code isActive = true} (desactivación activa)</li>
     *   <li><strong>Reactivación manual:</strong> El propietario marca como
     *       {@code isActive = false} antes de la fecha estimada</li>
     *   <li><strong>Finalización automática:</strong> El sistema puede marcar
     *       como {@code isActive = false} cuando se alcanza {@code disabledUntil}
     *       (lógica a implementar en servicio programado)</li>
     * </ol>
     *
     * <p><strong>Impacto en consultas:</strong></p>
     * <ul>
     *   <li>Solo se consideran desactivaciones con {@code isActive = true}
     *       para bloquear disponibilidad</li>
     *   <li>Las desactivaciones con {@code isActive = false} se mantienen
     *       en el historial para referencia futura</li>
     * </ul>
     *
     * <p><strong>Restricciones:</strong></p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>Valor por defecto: {@code true}</li>
     * </ul>
     *
     * <p>Ejemplo: {@code true} (alojamiento actualmente desactivado)</p>
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // =========================================================================
    // METADATOS DE AUDITORÍA
    // =========================================================================

    /**
     * Fecha y hora de creación del registro de desactivación.
     *
     * <p>Se establece automáticamente mediante la anotación
     * {@link CreationTimestamp} de Hibernate en el momento de persistir
     * la entidad por primera vez.</p>
     *
     * <p><strong>Propósito:</strong></p>
     * <ul>
     *   <li>Auditoría: saber cuándo se solicitó la desactivación</li>
     *   <li>Ordenamiento: mostrar historial cronológico</li>
     *   <li>Reportes: análisis de frecuencia de desactivaciones</li>
     * </ul>
     *
     * <p><strong>Restricciones:</strong></p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>No es actualizable ({@code updatable = false})</li>
     *   <li>Se establece automáticamente (no requiere asignación manual)</li>
     * </ul>
     *
     * <p><strong>Formato:</strong> ISO-8601 con zona horaria del servidor</p>
     *
     * <p>Ejemplo: {@code 2026-10-22T10:30:00}</p>
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última modificación del registro.
     *
     * <p>Se actualiza automáticamente mediante la anotación
     * {@link UpdateTimestamp} de Hibernate cada vez que se modifica
     * cualquier campo de la entidad.</p>
     *
     * <p><strong>Casos de actualización:</strong></p>
     * <ul>
     *   <li>Cambio en el motivo de desactivación</li>
     *   <li>Modificación de fechas (inicio o fin)</li>
     *   <li>Cambio de estado ({@code isActive})</li>
     * </ul>
     *
     * <p><strong>Propósito:</strong></p>
     * <ul>
     *   <li>Auditoría: rastrear cambios en la desactivación</li>
     *   <li>Sincronización: detectar registros modificados</li>
     *   <li>Debugging: entender secuencia de eventos</li>
     * </ul>
     *
     * <p><strong>Restricciones:</strong></p>
     * <ul>
     *   <li>Puede ser {@code null} inicialmente (antes de la primera modificación)</li>
     *   <li>Se actualiza automáticamente en cada operación de update</li>
     * </ul>
     *
     * <p><strong>Formato:</strong> ISO-8601 con zona horaria del servidor</p>
     *
     * <p>Ejemplo: {@code 2026-10-22T15:45:00} (modificado a las 3:45 PM)</p>
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================================================================
    // MÉTODOS AUXILIARES (Opcional pero recomendado)
    // =========================================================================

    /**
     * Verifica si la desactivación está vigente en una fecha específica.
     *
     * <p>Este método es útil para determinar si el alojamiento debería
     * estar bloqueado en un momento dado, considerando el rango temporal
     * y el estado activo.</p>
     *
     * @param fechaConsulta la fecha y hora a verificar
     * @return {@code true} si la desactivación está activa en esa fecha,
     *         {@code false} en caso contrario
     */
    public boolean isActiveOnDate(LocalDateTime fechaConsulta) {
        if (!isActive) {
            return false;
        }

        // Verificar que la fecha de consulta sea posterior o igual al inicio
        if (fechaConsulta.isBefore(disabledFrom)) {
            return false;
        }

        // Si hay fecha de fin, verificar que no la haya superado
        if (disabledUntil != null && fechaConsulta.isAfter(disabledUntil)) {
            return false;
        }

        return true;
    }

    /**
     * Calcula la duración estimada de la desactivación en días.
     *
     * <p>Útil para reportes y visualización de información al usuario.</p>
     *
     * @return número de días de duración, o {@code null} si la fecha de fin
     *         no está definida (duración indeterminada)
     */
    public Long getDurationInDays() {
        if (disabledUntil == null) {
            return null;
        }
        return java.time.Duration.between(disabledFrom, disabledUntil).toDays();
    }

    /**
     * Verifica si la desactivación ha expirado según la fecha actual.
     *
     * <p>Una desactivación se considera expirada si:</p>
     * <ul>
     *   <li>Tiene fecha de fin definida ({@code disabledUntil != null})</li>
     *   <li>La fecha actual es posterior a {@code disabledUntil}</li>
     *   <li>Está marcada como activa ({@code isActive = true})</li>
     * </ul>
     *
     * @return {@code true} si la desactivación debería haber finalizado,
     *         {@code false} si aún está vigente o no tiene fecha de fin
     */
    public boolean isExpired() {
        if (!isActive || disabledUntil == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(disabledUntil);
    }
}
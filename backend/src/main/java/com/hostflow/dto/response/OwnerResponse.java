package com.hostflow.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para la respuesta de datos de un dueño de alojamiento.
 *
 * <p>Se utiliza para devolver la información del dueño
 * al cliente sin exponer datos sensibles como la contraseña.</p>
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponse {

    /**
     * Identificador único del dueño.
     */
    private Long id;

    /**
     * Nombre completo del dueño.
     */
    private String fullName;

    /**
     * Nombre comercial del alojamiento.
     */
    private String accommodationName;

    /**
     * Correo electrónico del dueño.
     */
    private String email;

    /**
     * Indica si la cuenta está activa.
     */
    private Boolean enabled;

    /**
     * Roles asignados al dueño.
     */
    private List<String> roles;

    /**
     * Fecha de creación del registro.
     */
    private LocalDateTime createdAt;

    /**
     * Fecha de la última actualización.
     */
    private LocalDateTime updatedAt;
}
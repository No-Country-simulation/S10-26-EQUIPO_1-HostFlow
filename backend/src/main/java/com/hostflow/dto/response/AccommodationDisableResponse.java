package com.hostflow.dto.response;


import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO para la respuesta de desactivación temporal de alojamiento.
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationDisableResponse {

    private Long id;
    private Long ownerId;
    private String reason;
    private LocalDateTime disabledFrom;
    private LocalDateTime disabledUntil;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
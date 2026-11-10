package com.hostflow.service;

import com.hostflow.dto.response.AccommodationDisableResponse;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.AccommodationTemporaryDisable;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationTemporaryDisableRepository;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationTemporaryDisableService {

    private final AccommodationTemporaryDisableRepository disableRepository;
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // MÉTODOS PÚBLICOS (Retornan DTOs para la capa de presentación)
    // =========================================================================

    @Transactional
    public AccommodationDisableResponse disableAccommodationByEmail(
            String ownerEmail, String reason, LocalDateTime disabledFrom, LocalDateTime disabledUntil) {
        Owner owner = getOwnerByEmail(ownerEmail);
        AccommodationTemporaryDisable saved = disableAccommodationCore(owner.getId(), reason, disabledFrom, disabledUntil);
        return AccommodationDisableResponse.from(saved);
    }

    @Transactional
    public AccommodationDisableResponse enableAccommodationByEmail(String ownerEmail) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        AccommodationTemporaryDisable updated = enableAccommodationCore(ownerId);
        return AccommodationDisableResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public AccommodationDisableResponse getActiveDisableByEmail(String ownerEmail) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        AccommodationTemporaryDisable active = getActiveDisableCore(ownerId);
        // El método from() del DTO maneja null correctamente
        return AccommodationDisableResponse.from(active);
    }

    @Transactional(readOnly = true)
    public List<AccommodationDisableResponse> getDisableHistoryByEmail(String ownerEmail) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        return getDisableHistoryCore(ownerId).stream()
                .map(AccommodationDisableResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isAccommodationAvailableByEmail(String ownerEmail, LocalDateTime date) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        return isAccommodationAvailableCore(ownerId, date);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS - RESOLUCIÓN DE ENTIDADES
    // =========================================================================

    private Long getOwnerIdByEmail(String ownerEmail) {
        return getOwnerByEmail(ownerEmail).getId();
    }

    private Owner getOwnerByEmail(String ownerEmail) {
        return ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario con email: " + ownerEmail));
    }

    // =========================================================================
    // MÉTODOS PRIVADOS - LÓGICA DE NEGOCIO PURA (Trabajan con entidades)
    // =========================================================================

    private AccommodationTemporaryDisable disableAccommodationCore(
            Long ownerId, String reason, LocalDateTime disabledFrom, LocalDateTime disabledUntil) {

        if (disableRepository.findByOwnerIdAndIsActiveTrue(ownerId).isPresent()) {
            throw new BusinessException("Tu alojamiento ya tiene una desactivación temporal activa. Debes reactivarlo antes de crear una nueva.");
        }
        if (disabledFrom.isBefore(LocalDateTime.now())) {
            throw new BusinessException("La fecha de inicio de la desactivación no puede ser en el pasado.");
        }
        if (disabledUntil != null && disabledUntil.isBefore(disabledFrom)) {
            throw new BusinessException("La fecha de reactivación debe ser posterior a la fecha de desactivación.");
        }

        AccommodationTemporaryDisable disable = AccommodationTemporaryDisable.builder()
                .ownerId(ownerId)
                .reason(reason)
                .disabledFrom(disabledFrom)
                .disabledUntil(disabledUntil)
                .isActive(true)
                .build();

        AccommodationTemporaryDisable saved = disableRepository.save(disable);
        log.info("🚫 Alojamiento desactivado temporalmente - Owner ID: {}, Motivo: {}", ownerId, reason);
        return saved;
    }

    private AccommodationTemporaryDisable enableAccommodationCore(Long ownerId) {
        AccommodationTemporaryDisable disable = disableRepository.findByOwnerIdAndIsActiveTrue(ownerId)
                .orElseThrow(() -> new BusinessException("No hay una desactivación temporal activa para tu alojamiento."));

        disable.setIsActive(false);
        disable.setUpdatedAt(LocalDateTime.now());
        AccommodationTemporaryDisable updated = disableRepository.save(disable);
        log.info("✅ Alojamiento reactivado - Owner ID: {}", ownerId);
        return updated;
    }

    private AccommodationTemporaryDisable getActiveDisableCore(Long ownerId) {
        return disableRepository.findByOwnerIdAndIsActiveTrue(ownerId).orElse(null);
    }

    private List<AccommodationTemporaryDisable> getDisableHistoryCore(Long ownerId) {
        return disableRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    private boolean isAccommodationAvailableCore(Long ownerId, LocalDateTime date) {
        return !disableRepository.findActiveDisableAtDate(ownerId, date).isPresent();
    }
}
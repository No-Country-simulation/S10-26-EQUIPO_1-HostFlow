package com.hostflow.service;

import com.hostflow.dto.request.CreateGuestRequest;
import com.hostflow.dto.response.GuestResponse;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Guest;
import com.hostflow.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de huéspedes (datos personales del onboarding).
 *
 * <p>HostFlow <strong>no</strong> gestiona métodos de pago del huésped. Los métodos
 * de pago que se informan en el flujo de reservas son los del alojamiento y solo
 * tienen carácter informativo; el pago ocurre fuera de la plataforma.</p>
 *
 * @author HostFlow Team
 * @version 3.0.0 (Eliminado submódulo GuestPaymentMethod — fuera de alcance)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestService {

    private final GuestRepository guestRepository;

    /**
     * Crea un nuevo huésped o reutiliza el registro existente por documento de identidad.
     * El correo es un dato de contacto y puede repetirse entre huéspedes y reservas.
     */
    @Transactional
    public Guest createOrFindGuest(CreateGuestRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedDocument = request.documentNumber() == null || request.documentNumber().isBlank()
                ? null : request.documentNumber().trim();

        if (normalizedDocument != null) {
            Guest existingGuest = guestRepository.findByDocumentNumber(normalizedDocument).orElse(null);
            if (existingGuest != null) {
                if (existingGuest.getEmail() != null
                        && !existingGuest.getEmail().equalsIgnoreCase(normalizedEmail)) {
                    throw new BusinessException(
                            "El documento ya está registrado con otro correo electrónico.");
                }
                boolean updated = false;
                if (request.preferences() != null && !request.preferences().isBlank()) {
                    existingGuest.setPreferences(request.preferences().trim());
                    updated = true;
                }
                if (request.allergies() != null && !request.allergies().isEmpty()) {
                    existingGuest.setAllergies(request.allergies());
                    updated = true;
                }
                return updated ? guestRepository.save(existingGuest) : existingGuest;
            }
        }

        String firstName = request.firstName() != null ? request.firstName().trim() : "";
        String lastName = request.lastName() != null ? request.lastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();

        Guest newGuest = Guest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .fullName(fullName)
                .email(normalizedEmail)
                .phone(request.phone() != null ? request.phone().trim() : null)
                .documentNumber(normalizedDocument)
                .preferences(request.preferences() != null && !request.preferences().isBlank()
                        ? request.preferences().trim() : null)
                .allergies(request.allergies())
                .active(true)
                .build();
        return guestRepository.save(newGuest);
    }

    @Transactional(readOnly = true)
    public GuestResponse getGuestById(Long guestId) {
        return guestRepository.findById(guestId)
                .map(GuestResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el huésped con ID: " + guestId));
    }

    @Transactional(readOnly = true)
    public GuestResponse getGuestByEmail(String email) {
        return guestRepository.findFirstByEmailOrderByIdDesc(email.trim().toLowerCase())
                .map(GuestResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el huésped con email: " + email));
    }

    /**
     * Acceso a la entidad para otros servicios (p. ej. asociación en onboarding de reserva).
     */
    @Transactional(readOnly = true)
    public Guest getGuestEntityById(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el huésped con ID: " + guestId));
    }
}

package com.hostflow.service;

import com.hostflow.dto.request.CreateGuestRequest;
import com.hostflow.dto.request.GuestPaymentMethodRequest;
import com.hostflow.dto.response.GuestPaymentMethodResponse;
import com.hostflow.dto.response.GuestResponse;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Guest;
import com.hostflow.model.GuestPaymentMethod;
import com.hostflow.repository.GuestPaymentMethodRepository;
import com.hostflow.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio central para la gestión de huéspedes (clientes) y sus métodos de pago en HostFlow.
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.6.0):</strong></p>
 * <ul>
 *   <li>Los métodos de lectura retornan DTOs ({@link GuestResponse} y {@link GuestPaymentMethodResponse})
 *       en lugar de entidades JPA, evitando {@code LazyInitializationException}.</li>
 *   <li>Se han creado métodos auxiliares públicos (ej. {@code getPaymentMethodEntity}) que retornan
 *       la entidad directamente (lanzando excepción si no existe). Esto permite que otros servicios
 *       (como {@code ReservationService}) accedan a ellas de forma limpia y segura.</li>
 *   <li>Se eliminó el uso innecesario de {@code Optional} en métodos de servicio a servicio,
 *       centralizando la validación de existencia y propiedad en un solo lugar (Principio DRY).</li>
 * </ul>
 *
 * <p><strong>Política de transacciones:</strong> Los métodos de escritura usan
 * {@code @Transactional} para garantizar la atomicidad, mientras que los de lectura
 * usan {@code @Transactional(readOnly = true)} para optimizar el rendimiento.</p>
 *
 * @author HostFlow Team
 * @version 2.6.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    private final GuestRepository guestRepository;
    private final GuestPaymentMethodRepository paymentMethodRepository;

    // =========================================================================
    // GESTIÓN DE HUÉSPEDES
    // =========================================================================

    /**
     * Crea un nuevo huésped o retorna uno existente por su correo electrónico.
     *
     * <p><strong>⚠️ Excepción de Diseño:</strong> Este método retorna la entidad {@link Guest}
     * (no un DTO) porque es utilizado internamente por {@code ReservationService} para
     * asociar el huésped a una nueva reserva ({@code reservation.setGuest(guest)}).
     * Para la capa web (Controladores), utilice {@link #getGuestById} o {@link #getGuestByEmail}.</p>
     *
     * @param request los datos del huésped a crear o buscar
     * @return la entidad Guest existente (actualizada) o recién creada
     * @throws BusinessException si el documento de identidad ya está registrado
     */
    @Transactional
    public Guest createOrFindGuest(CreateGuestRequest request) {
        log.info("🔍 Buscando huésped por email: {}", request.email());

        return guestRepository.findByEmail(request.email())
                .map(existingGuest -> {
                    log.info("✅ Huésped encontrado: {} (ID: {})", existingGuest.getFullName(), existingGuest.getId());

                    boolean updated = false;
                    if (request.preferences() != null && !request.preferences().isBlank()) {
                        existingGuest.setPreferences(request.preferences().trim());
                        updated = true;
                    }
                    if (request.allergies() != null && !request.allergies().isEmpty()) {
                        existingGuest.setAllergies(request.allergies());
                        updated = true;
                    }

                    if (updated) {
                        return guestRepository.save(existingGuest);
                    }
                    return existingGuest;
                })
                .orElseGet(() -> {
                    if (request.documentNumber() != null && !request.documentNumber().isBlank()) {
                        String normalizedDoc = request.documentNumber().trim();
                        guestRepository.findByDocumentNumber(normalizedDoc)
                                .ifPresent(guestWithSameDoc -> {
                                    throw new BusinessException(
                                            "Ya existe un huésped registrado con el documento: " + normalizedDoc);
                                });
                    }

                    String firstName = request.firstName() != null ? request.firstName().trim() : "";
                    String lastName = request.lastName() != null ? request.lastName().trim() : "";
                    String fullName = (firstName + " " + lastName).trim();

                    Guest newGuest = Guest.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .fullName(fullName)
                            .email(request.email().trim().toLowerCase())
                            .phone(request.phone() != null ? request.phone().trim() : null)
                            .documentNumber(request.documentNumber() != null ? request.documentNumber().trim() : null)
                            .preferences(request.preferences() != null && !request.preferences().isBlank() ? request.preferences().trim() : null)
                            .allergies(request.allergies())
                            .active(true)
                            .build();

                    Guest savedGuest = guestRepository.save(newGuest);
                    log.info("✅ Nuevo huésped creado: {} (ID: {})", savedGuest.getFullName(), savedGuest.getId());
                    return savedGuest;
                });
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
        return guestRepository.findByEmail(email.trim().toLowerCase())
                .map(GuestResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el huésped con email: " + email));
    }

    // =========================================================================
    // GESTIÓN DE MÉTODOS DE PAGO
    // =========================================================================

    @Transactional
    public GuestPaymentMethodResponse createPaymentMethod(Long guestId, GuestPaymentMethodRequest request) {
        Guest guest = getGuestEntityById(guestId);

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault())
                || !paymentMethodRepository.existsByGuest_IdAndDefaultMethodTrue(guestId);

        if (makeDefault) {
            paymentMethodRepository.clearDefaultForGuest(guestId);
        }

        GuestPaymentMethod method = GuestPaymentMethod.builder()
                .guest(guest)
                .paymentType(request.paymentType())
                .displayName(request.displayName().trim())
                .providerName(request.providerName())
                .accountNumber(request.accountNumber())
                .accountHolder(request.accountHolder())
                .expirationDate(request.expirationDate())
                .defaultMethod(makeDefault)
                .active(true)
                .build();

        GuestPaymentMethod saved = paymentMethodRepository.save(method);
        log.info("💳 Método de pago creado para huésped {}: {}", guestId, saved.getDisplayName());

        return GuestPaymentMethodResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GuestPaymentMethodResponse> listPaymentMethods(Long guestId) {
        return paymentMethodRepository.findAllByGuest_IdOrderByDefaultMethodDescCreatedAtAsc(guestId)
                .stream()
                .map(GuestPaymentMethodResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuestPaymentMethodResponse getDefaultPaymentMethod(Long guestId) {
        return paymentMethodRepository.findByGuest_IdAndDefaultMethodTrue(guestId)
                .map(GuestPaymentMethodResponse::from)
                .orElseThrow(() -> new BusinessException(
                        "El huésped no tiene un método de pago predeterminado configurado."));
    }

    @Transactional(readOnly = true)
    public boolean hasActivePaymentMethod(Long guestId) {
        return paymentMethodRepository.existsByGuest_IdAndActiveTrue(guestId);
    }

    @Transactional
    public void deletePaymentMethod(Long guestId, Long methodId) {
        // ✅ El método getPaymentMethodEntity ya lanza la excepción si no existe, simplificando el código.
        GuestPaymentMethod method = getPaymentMethodEntity(guestId, methodId);

        if (Boolean.TRUE.equals(method.isDefault())) {
            paymentMethodRepository.clearDefaultForGuest(guestId);
        }

        paymentMethodRepository.delete(method);
        log.info("🗑️ Método de pago eliminado para huésped {}: {}", guestId, method.getDisplayName());
    }

    @Transactional(readOnly = true)
    public GuestPaymentMethodResponse getPaymentMethod(Long guestId, Long methodId) {
        // ✅ Obtenemos la entidad directamente (la validación de existencia está centralizada)
        GuestPaymentMethod method = getPaymentMethodEntity(guestId, methodId);
        return GuestPaymentMethodResponse.from(method);
    }

    // =========================================================================
    // 🆕 MÉTODOS PÚBLICOS DE ENTIDAD (Para uso entre servicios)
    // =========================================================================

    /**
     * Obtiene la entidad Guest por su ID.
     *
     * <p><strong>🆕 Método público:</strong> Permite que otros servicios (como
     * {@code ReservationService}) accedan a la entidad para operaciones de
     * escritura o asociación (ej. {@code reservation.setGuest(guest)}).</p>
     */
    @Transactional(readOnly = true)
    public Guest getGuestEntityById(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el huésped con ID: " + guestId));
    }

    /**
     * Obtiene la entidad GuestPaymentMethod por su ID, validando que pertenezca al huésped.
     *
     * <p><strong>🆕 Método público:</strong> Permite que otros servicios (como
     * {@code ReservationService}) accedan a la entidad para operaciones de
     * escritura o validación (ej. {@code reservation.setPaymentMethod(paymentMethod)}).
     * Retorna la entidad directamente, lanzando excepción si no existe, para evitar
     * el manejo redundante de {@code Optional} en los servicios consumidores.</p>
     *
     * @param guestId  ID del huésped propietario
     * @param methodId ID del método de pago
     * @return la entidad GuestPaymentMethod
     * @throws ResourceNotFoundException si no existe o no pertenece al huésped especificado
     */
    @Transactional(readOnly = true)
    public GuestPaymentMethod getPaymentMethodEntity(Long guestId, Long methodId) {
        return paymentMethodRepository.findById(methodId)
                .filter(method -> method.getGuest().getId().equals(guestId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el método de pago con ID: " + methodId +
                                " o no pertenece al huésped especificado."));
    }
}
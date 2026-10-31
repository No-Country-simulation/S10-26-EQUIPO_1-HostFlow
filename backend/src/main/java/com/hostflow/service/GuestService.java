package com.hostflow.service;

import com.hostflow.dto.request.CreateGuestRequest;
import com.hostflow.dto.request.GuestPaymentMethodRequest;
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
 * Servicio para gestionar huéspedes (clientes) y sus métodos de pago en HostFlow.
 *
 * <p>Responsabilidades principales:</p>
 * <ul>
 *   <li>Crear o buscar huéspedes por correo electrónico, validando unicidad de documentos.</li>
 *   <li>Registrar, listar y eliminar métodos de pago asociados a un huésped.</li>
 *   <li>Validar que existan métodos de pago activos antes de permitir una reserva.</li>
 * </ul>
 *
 * <p><strong>Política de transacciones:</strong> Los métodos de escritura usan
 * {@code @Transactional} para garantizar la atomicidad, mientras que los de lectura
 * usan {@code @Transactional(readOnly = true)} para optimizar el rendimiento.</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestService {

    private final GuestRepository guestRepository;
    private final GuestPaymentMethodRepository paymentMethodRepository;

    // =========================================================================
    // GESTIÓN DE HUÉSPEDES
    // =========================================================================

    /**
     * Crea un nuevo huésped o retorna uno existente por su correo electrónico.
     *
     * <p><strong>Flujo del proceso:</strong></p>
     * <ol>
     *   <li>Busca si ya existe un huésped con el email proporcionado.</li>
     *   <li>Si existe, lo retorna inmediatamente (idempotencia).</li>
     *   <li>Si no existe, valida que el número de documento (si se proporciona)
     *       no esté ya registrado en el sistema.</li>
     *   <li>Crea y persiste el nuevo huésped con los datos normalizados (trim, lowercase).</li>
     * </ol>
     *
     * @param request los datos del huésped a crear o buscar (usando record)
     * @return el huésped existente o recién creado
     * @throws BusinessException si el documento de identidad ya está registrado
     */
    @Transactional
    public Guest createOrFindGuest(CreateGuestRequest request) {
        log.info("🔍 Buscando huésped por email: {}", request.email());

        return guestRepository.findByEmail(request.email())
                .map(existingGuest -> {
                    log.info("✅ Huésped encontrado: {} (ID: {})",
                            existingGuest.getFullName(), existingGuest.getId());
                    return existingGuest;
                })
                .orElseGet(() -> {
                    // 1. Validar que el documento no exista si fue proporcionado
                    if (request.documentNumber() != null && !request.documentNumber().isBlank()) {
                        String normalizedDoc = request.documentNumber().trim();
                        guestRepository.findByDocumentNumber(normalizedDoc)
                                .ifPresent(guestWithSameDoc -> {
                                    throw new BusinessException(
                                            "Ya existe un huésped registrado con el documento: " + normalizedDoc);
                                });
                    }

                    // 2. Crear nuevo huésped con datos normalizados
                    Guest newGuest = Guest.builder()
                            .firstName(request.firstName().trim())
                            .lastName(request.lastName().trim())
                            .email(request.email().trim().toLowerCase())
                            .phone(request.phone() != null ? request.phone().trim() : null)
                            .documentNumber(request.documentNumber() != null ?
                                    request.documentNumber().trim() : null)
                            .active(true)
                            .build();

                    Guest savedGuest = guestRepository.save(newGuest);
                    log.info("✅ Nuevo huésped creado: {} (ID: {})",
                            savedGuest.getFullName(), savedGuest.getId());
                    return savedGuest;
                });
    }

    /**
     * Busca un huésped por su identificador único.
     *
     * @param guestId ID del huésped
     * @return el huésped encontrado
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public Guest getGuestById(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el huésped con ID: " + guestId));
    }

    /**
     * Busca un huésped por su correo electrónico.
     *
     * @param email correo electrónico del huésped
     * @return el huésped encontrado
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public Guest getGuestByEmail(String email) {
        return guestRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el huésped con email: " + email));
    }

    // =========================================================================
    // GESTIÓN DE MÉTODOS DE PAGO
    // =========================================================================

    /**
     * Crea un nuevo método de pago para un huésped.
     *
     * <p><strong>Regla de negocio:</strong> Si no existe un método predeterminado
     * para este huésped, o si se solicita explícitamente, este nuevo método se
     * marcará como predeterminado, limpiando el flag de los métodos anteriores.</p>
     *
     * @param guestId ID del huésped propietario del método
     * @param request datos del método de pago a registrar
     * @return el método de pago creado y persistido
     */
    @Transactional
    public GuestPaymentMethod createPaymentMethod(Long guestId, GuestPaymentMethodRequest request) {
        Guest guest = getGuestById(guestId);

        // Determinar si debe ser el método predeterminado
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault())
                || !paymentMethodRepository.existsByGuest_IdAndDefaultMethodTrue(guestId);

        // Si será predeterminado, limpiar el flag de los métodos existentes
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
        return saved;
    }

    /**
     * Lista todos los métodos de pago de un huésped.
     *
     * <p>La lista se ordena colocando primero el método predeterminado,
     * y luego el resto ordenado por fecha de creación.</p>
     *
     * @param guestId ID del huésped
     * @return lista de métodos de pago ordenados
     */
    @Transactional(readOnly = true)
    public List<GuestPaymentMethod> listPaymentMethods(Long guestId) {
        return paymentMethodRepository.findAllByGuest_IdOrderByDefaultMethodDescCreatedAtAsc(guestId);
    }

    /**
     * Obtiene el método de pago predeterminado de un huésped.
     *
     * @param guestId ID del huésped
     * @return el método de pago marcado como predeterminado
     * @throws BusinessException si el huésped no tiene un método predeterminado configurado
     */
    @Transactional(readOnly = true)
    public GuestPaymentMethod getDefaultPaymentMethod(Long guestId) {
        return paymentMethodRepository.findByGuest_IdAndDefaultMethodTrue(guestId)
                .orElseThrow(() -> new BusinessException(
                        "El huésped no tiene un método de pago predeterminado configurado."));
    }

    /**
     * Verifica si un huésped tiene al menos un método de pago activo.
     *
     * <p>Esta validación es crítica antes de permitir la creación de una reserva.</p>
     *
     * @param guestId ID del huésped
     * @return {@code true} si tiene al menos un método activo, {@code false} en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean hasActivePaymentMethod(Long guestId) {
        return paymentMethodRepository.existsByGuest_IdAndActiveTrue(guestId);
    }

    /**
     * Elimina un método de pago de un huésped.
     *
     * <p><strong>Regla de negocio:</strong> Si el método eliminado era el predeterminado,
     * se limpia el flag en la base de datos para que el sistema pueda asignar
     * automáticamente otro método como predeterminado en el futuro.</p>
     *
     * @param guestId  ID del huésped
     * @param methodId ID del método de pago a eliminar
     * @throws ResourceNotFoundException si el método no existe o no pertenece al huésped
     */
    @Transactional
    public void deletePaymentMethod(Long guestId, Long methodId) {
        GuestPaymentMethod method = getPaymentMethod(guestId, methodId);

        // Si era el predeterminado, limpiar el flag para mantener la integridad
        if (method.isDefault()) {
            paymentMethodRepository.clearDefaultForGuest(guestId);
        }

        paymentMethodRepository.delete(method);
        log.info("🗑️ Método de pago eliminado para huésped {}: {}", guestId, method.getDisplayName());
    }

    /**
     * Busca un método de pago por su ID, validando estrictamente que pertenezca al huésped indicado.
     *
     * <p>Esta validación de propiedad es crucial para evitar que un huésped
     * utilice o elimine métodos de pago de otro usuario.</p>
     *
     * @param guestId  ID del huésped propietario
     * @param methodId ID del método de pago
     * @return el método de pago encontrado
     * @throws ResourceNotFoundException si no existe o no pertenece al huésped especificado
     */
    @Transactional(readOnly = true)
    public GuestPaymentMethod getPaymentMethod(Long guestId, Long methodId) {
        return paymentMethodRepository.findById(methodId)
                .filter(m -> m.getGuest().getId().equals(guestId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el método de pago con ID: " + methodId +
                                " o no pertenece al huésped especificado."));
    }
}
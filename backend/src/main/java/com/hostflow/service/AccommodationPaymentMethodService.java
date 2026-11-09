package com.hostflow.service;

import com.hostflow.dto.request.AccommodationPaymentMethodRequest;
import com.hostflow.dto.response.AccommodationPaymentMethodResponse;
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationPaymentMethod;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationPaymentMethodRepository;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para gestionar los métodos de pago del alojamiento del propietario.
 *
 * <p>Permite al propietario registrar, actualizar y eliminar métodos de pago
 * que recibirán los pagos de los huéspedes (transferencias bancarias,
 * billeteras digitales, efectivo, tarjetas, etc.).</p>
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.3):</strong></p>
 * <ul>
 *   <li>Todos los métodos de lectura y escritura ahora retornan DTOs
 *       ({@link AccommodationPaymentMethodResponse}) en lugar de entidades JPA,
 *       garantizando que las colecciones se carguen correctamente dentro de la transacción
 *       y evitando {@code LazyInitializationException}.</li>
 *   <li>Se agregaron métodos que aceptan el {@code ownerEmail} en lugar del {@code ownerId},
 *       delegando la resolución del Owner al servicio y eliminando la necesidad de que
 *       los controladores inyecten repositorios directamente.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.3.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationPaymentMethodService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================
    private final AccommodationRepository accommodationRepository;
    private final AccommodationPaymentMethodRepository paymentMethodRepository;
    private final OwnerRepository ownerRepository;
    private final EmailService emailService;

    // =========================================================================
    // 🆕 MÉTODOS PÚBLICOS QUE ACEPTAN EMAIL (Para controladores refactorizados)
    // =========================================================================

    /**
     * Lista todos los métodos de pago del alojamiento del propietario.
     *
     * @param ownerEmail el email del propietario autenticado
     * @return lista de DTOs de métodos de pago del alojamiento
     */
    @Transactional(readOnly = true)
    public List<AccommodationPaymentMethodResponse> listByEmail(String ownerEmail) {
        Long ownerId = getAccommodationByEmail(ownerEmail).getOwner().getId();
        return list(ownerId);
    }

    /**
     * Crea un nuevo método de pago para el alojamiento del propietario.
     *
     * @param ownerEmail el email del propietario autenticado
     * @param request los datos del método de pago a crear
     * @return el DTO del método de pago creado
     */
    @Transactional
    public AccommodationPaymentMethodResponse createByEmail(String ownerEmail, AccommodationPaymentMethodRequest request) {
        Long ownerId = getAccommodationByEmail(ownerEmail).getOwner().getId();
        return create(ownerId, request);
    }

    /**
     * Actualiza parcialmente un método de pago existente.
     *
     * @param ownerEmail el email del propietario autenticado
     * @param methodId el identificador del método de pago a actualizar
     * @param request los datos a actualizar
     * @return el DTO del método de pago actualizado
     */
    @Transactional
    public AccommodationPaymentMethodResponse updateByEmail(String ownerEmail, Long methodId, AccommodationPaymentMethodRequest request) {
        Long ownerId = getAccommodationByEmail(ownerEmail).getOwner().getId();
        return update(ownerId, methodId, request);
    }

    /**
     * Elimina un método de pago del alojamiento.
     *
     * @param ownerEmail el email del propietario autenticado
     * @param methodId el identificador del método de pago a eliminar
     */
    @Transactional
    public void deleteByEmail(String ownerEmail, Long methodId) {
        Long ownerId = getAccommodationByEmail(ownerEmail).getOwner().getId();
        delete(ownerId, methodId);
    }

    // =========================================================================
    // MÉTODOS ORIGINALES (Se mantienen para compatibilidad con tests y retornan DTOs)
    // =========================================================================

    /**
     * Lista todos los métodos de pago del alojamiento del propietario.
     *
     * @param ownerId el identificador único del propietario autenticado
     * @return lista de DTOs de métodos de pago del alojamiento
     */
    @Transactional(readOnly = true)
    public List<AccommodationPaymentMethodResponse> list(Long ownerId) {
        Accommodation accommodation = getAccommodation(ownerId);
        return paymentMethodRepository.findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(
                        accommodation.getId()).stream()
                .map(AccommodationPaymentMethodResponse::from)
                .toList();
    }

    /**
     * Crea un nuevo método de pago para el alojamiento del propietario.
     *
     * @param ownerId el identificador único del propietario autenticado
     * @param request los datos del método de pago a crear
     * @return el DTO del método de pago creado
     * @throws BusinessException si faltan datos obligatorios
     */
    @Transactional
    public AccommodationPaymentMethodResponse create(Long ownerId, AccommodationPaymentMethodRequest request) {
        // 1. Validación de campos obligatorios básicos
        if (request.getPaymentType() == null || request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new BusinessException("paymentType y displayName son obligatorios.");
        }

        // 2. Obtener el alojamiento del propietario
        Accommodation accommodation = getAccommodation(ownerId);

        // 3. Determinar si debe ser el método predeterminado
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || !paymentMethodRepository.existsByAccommodation_IdAndDefaultMethodTrue(accommodation.getId());

        // 4. Si será predeterminado, limpiar el flag de los demás métodos
        if (makeDefault) {
            paymentMethodRepository.clearDefaultForAccommodation(accommodation.getId());
        }

        // 5. Construir y configurar el nuevo método de pago
        AccommodationPaymentMethod method = new AccommodationPaymentMethod();
        method.setAccommodation(accommodation);
        method.setPaymentType(request.getPaymentType());
        method.setDisplayName(request.getDisplayName().trim());
        applyOptionalFields(method, request);
        method.setDefault(makeDefault);
        method.setActive(request.getIsActive() == null || request.getIsActive());

        // 6. Validar campos obligatorios específicos por tipo de pago
        validateRequiredFieldsByType(method);

        // 7. Persistir en la base de datos
        AccommodationPaymentMethod saved = paymentMethodRepository.save(method);

        log.info("💳 Método de pago creado: '{}' ({}) para alojamiento '{}' (ID: {})",
                saved.getDisplayName(), saved.getPaymentType(),
                accommodation.getName(), accommodation.getId());

        // 8. Enviar email de confirmación al propietario (Política de resiliencia)
        sendPaymentMethodCreatedEmail(accommodation, saved);

        // 9. Retornar DTO mapeado dentro de la transacción
        return AccommodationPaymentMethodResponse.from(saved);
    }

    /**
     * Actualiza parcialmente un método de pago existente.
     *
     * @param ownerId  el identificador único del propietario autenticado
     * @param methodId el identificador del método de pago a actualizar
     * @param request  los datos a actualizar (solo campos no nulos)
     * @return el DTO del método de pago actualizado
     * @throws BusinessException si la solicitud está vacía o displayName es vacío
     * @throws ResourceNotFoundException si el método no existe o no pertenece al alojamiento
     */
    @Transactional
    public AccommodationPaymentMethodResponse update(Long ownerId, Long methodId, AccommodationPaymentMethodRequest request) {
        if (isEmptyPatch(request)) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar.");
        }

        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository.findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ese método de pago en tu alojamiento."));

        if (request.getDisplayName() != null && request.getDisplayName().isBlank()) {
            throw new BusinessException("displayName no puede estar vacío.");
        }

        // Manejo del flag de método predeterminado
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            paymentMethodRepository.clearDefaultForAccommodation(accommodation.getId());
            method.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            method.setDefault(false);
        }

        // Aplicar actualizaciones de campos opcionales
        if (request.getPaymentType() != null) method.setPaymentType(request.getPaymentType());
        if (request.getDisplayName() != null) method.setDisplayName(request.getDisplayName().trim());
        if (request.getProviderName() != null) method.setProviderName(blankToNull(request.getProviderName()));
        if (request.getBankName() != null) method.setBankName(blankToNull(request.getBankName()));
        if (request.getAccountHolder() != null) method.setAccountHolder(blankToNull(request.getAccountHolder()));
        if (request.getAccountType() != null) method.setAccountType(blankToNull(request.getAccountType()));
        if (request.getAccountNumber() != null) method.setAccountNumber(blankToNull(request.getAccountNumber()));
        if (request.getCbuCvu() != null) method.setCbuCvu(blankToNull(request.getCbuCvu()));
        if (request.getAlias() != null) method.setAlias(blankToNull(request.getAlias()));
        if (request.getTaxId() != null) method.setTaxId(blankToNull(request.getTaxId()));
        if (request.getUserIdentifier() != null) method.setUserIdentifier(blankToNull(request.getUserIdentifier()));
        if (request.getWalletPhone() != null) method.setWalletPhone(blankToNull(request.getWalletPhone()));
        if (request.getWalletEmail() != null) method.setWalletEmail(blankToNull(request.getWalletEmail()));
        if (request.getInstructions() != null) method.setInstructions(blankToNull(request.getInstructions()));
        if (request.getIsActive() != null) method.setActive(request.getIsActive());

        // Validar que el estado final cumpla con las reglas del tipo de pago
        validateRequiredFieldsByType(method);

        AccommodationPaymentMethod saved = paymentMethodRepository.save(method);

        // Retornar DTO mapeado dentro de la transacción
        return AccommodationPaymentMethodResponse.from(saved);
    }

    /**
     * Elimina un método de pago del alojamiento.
     *
     * @param ownerId  el identificador único del propietario autenticado
     * @param methodId el identificador del método de pago a eliminar
     * @throws ResourceNotFoundException si el método no existe o no pertenece al alojamiento
     */
    @Transactional
    public void delete(Long ownerId, Long methodId) {
        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository.findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ese método de pago en tu alojamiento."));

        boolean wasDefault = method.isDefault();
        paymentMethodRepository.delete(method);

        // Si era el predeterminado, asignar otro automáticamente
        if (wasDefault) {
            List<AccommodationPaymentMethod> remaining = paymentMethodRepository
                    .findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(accommodation.getId());

            if (!remaining.isEmpty()) {
                AccommodationPaymentMethod nextDefault = remaining.get(0);
                nextDefault.setDefault(true);
                paymentMethodRepository.save(nextDefault);
                log.info("🔄 Nuevo método predeterminado asignado: '{}' (ID: {})",
                        nextDefault.getDisplayName(), nextDefault.getId());
            }
        }

        log.info("🗑️ Método de pago eliminado: '{}' (ID: {}) del alojamiento '{}'",
                method.getDisplayName(), methodId, accommodation.getName());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Envía el correo electrónico de confirmación al propietario
     * cuando se crea un nuevo método de pago.
     *
     * <p><strong>Política de resiliencia:</strong> Si falla el envío del email,
     * solo se registra el error en los logs, pero NO se revierte la creación
     * del método de pago (la transacción ya está comprometida).</p>
     */
    private void sendPaymentMethodCreatedEmail(Accommodation accommodation, AccommodationPaymentMethod method) {
        try {
            Owner owner = accommodation.getOwner();
            emailService.sendPaymentMethodCreatedEmail(
                    owner.getEmail(),
                    owner.getFullName(),
                    accommodation.getName(),
                    method.getDisplayName(),
                    method.getPaymentType().name(),
                    method.isDefault()
            );
            log.info("📧 Email de confirmación de método de pago enviado a: {}", owner.getEmail());
        } catch (Exception e) {
            log.error("⚠️ No se pudo enviar el email de confirmación de método de pago: {}", e.getMessage(), e);
        }
    }

    /**
     * Obtiene el alojamiento asociado al email del propietario.
     *
     * <p>Este método centraliza la lógica de búsqueda del Owner y su Accommodation,
     * evitando duplicación en los controladores.</p>
     */
    private Accommodation getAccommodationByEmail(String ownerEmail) {
        Owner owner = ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario con email: " + ownerEmail));

        return accommodationRepository.findByOwner_Id(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento."));
    }

    /**
     * Obtiene el alojamiento asociado al ID del propietario.
     */
    private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Registra primero los datos generales del alojamiento."));
    }

    /**
     * Verifica si la solicitud de actualización (PATCH) está completamente vacía.
     */
    private boolean isEmptyPatch(AccommodationPaymentMethodRequest r) {
        return r.getPaymentType() == null && r.getDisplayName() == null && r.getProviderName() == null
                && r.getBankName() == null && r.getAccountHolder() == null && r.getAccountType() == null
                && r.getAccountNumber() == null && r.getCbuCvu() == null && r.getAlias() == null && r.getTaxId() == null
                && r.getUserIdentifier() == null && r.getWalletPhone() == null && r.getWalletEmail() == null
                && r.getInstructions() == null && r.getIsDefault() == null && r.getIsActive() == null;
    }

    /**
     * Aplica los campos opcionales a la entidad, convirtiendo strings vacíos a null.
     */
    private void applyOptionalFields(AccommodationPaymentMethod m, AccommodationPaymentMethodRequest r) {
        m.setProviderName(blankToNull(r.getProviderName()));
        m.setBankName(blankToNull(r.getBankName()));
        m.setAccountHolder(blankToNull(r.getAccountHolder()));
        m.setAccountType(blankToNull(r.getAccountType()));
        m.setAccountNumber(blankToNull(r.getAccountNumber()));
        m.setCbuCvu(blankToNull(r.getCbuCvu()));
        m.setAlias(blankToNull(r.getAlias()));
        m.setTaxId(blankToNull(r.getTaxId()));
        m.setUserIdentifier(blankToNull(r.getUserIdentifier()));
        m.setWalletPhone(blankToNull(r.getWalletPhone()));
        m.setWalletEmail(blankToNull(r.getWalletEmail()));
        m.setInstructions(blankToNull(r.getInstructions()));
    }

    /**
     * Valida los datos obligatorios según el tipo del método de pago.
     * Se ejecuta después de aplicar los campos para que PATCH valide el estado final.
     */
    private void validateRequiredFieldsByType(AccommodationPaymentMethod method) {
        if (method.getPaymentType() == PaymentMethodType.BANK_TRANSFER) {
            requireField(method.getBankName(), "bankName", "BANK_TRANSFER");
            requireField(method.getAccountHolder(), "accountHolder", "BANK_TRANSFER");
            requireField(method.getAccountType(), "accountType", "BANK_TRANSFER");
            requireField(method.getAccountNumber(), "accountNumber", "BANK_TRANSFER");
            requireField(method.getCbuCvu(), "cbuCvu", "BANK_TRANSFER");
            requireField(method.getAlias(), "alias", "BANK_TRANSFER");
        } else if (method.getPaymentType() == PaymentMethodType.DIGITAL_WALLET) {
            requireField(method.getAccountHolder(), "accountHolder", "DIGITAL_WALLET");
            requireField(method.getUserIdentifier(), "userIdentifier", "DIGITAL_WALLET");
            requireField(method.getWalletPhone(), "walletPhone", "DIGITAL_WALLET");
            requireField(method.getWalletEmail(), "walletEmail", "DIGITAL_WALLET");
            if (!isValidEmail(method.getWalletEmail())) {
                throw new BusinessException("walletEmail debe ser un correo electrónico válido para DIGITAL_WALLET.");
            }
        }
    }

    /**
     * Lanza una excepción si un campo obligatorio está vacío.
     */
    private void requireField(String value, String fieldName, String paymentType) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " es obligatorio para " + paymentType + ".");
        }
    }

    /**
     * Valida el formato básico de un correo electrónico.
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    /**
     * Convierte un string vacío o en blanco a null, o lo trimmea si tiene contenido.
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
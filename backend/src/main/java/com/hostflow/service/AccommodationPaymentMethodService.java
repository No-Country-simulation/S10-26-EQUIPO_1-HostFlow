package com.hostflow.service;

import com.hostflow.dto.request.AccommodationPaymentMethodRequest;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationPaymentMethod;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationPaymentMethodRepository;
import com.hostflow.repository.AccommodationRepository;
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
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Soporte para múltiples métodos de pago por alojamiento.</li>
 *   <li>Un método puede marcarse como predeterminado.</li>
 *   <li>Al eliminar el método predeterminado, se asigna automáticamente otro.</li>
 *   <li>🆕 Envía email de confirmación al propietario al crear un nuevo método.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationPaymentMethodService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationPaymentMethodRepository paymentMethodRepository;
    /** 🆕 Servicio para el envío de correos electrónicos de confirmación. */
    private final EmailService emailService;

    // =========================================================================
    // LISTAR MÉTODOS DE PAGO
    // =========================================================================

    /**
     * Lista todos los métodos de pago del alojamiento del propietario,
     * ordenados con el predeterminado primero.
     *
     * @param ownerId el identificador único del propietario autenticado
     * @return lista de métodos de pago del alojamiento
     */
    @Transactional(readOnly = true)
    public List<AccommodationPaymentMethod> list(Long ownerId) {
        Accommodation accommodation = getAccommodation(ownerId);
        return paymentMethodRepository
                .findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(
                        accommodation.getId());
    }

    // =========================================================================
    // 🆕 CREAR MÉTODO DE PAGO (con envío de email)
    // =========================================================================

    /**
     * Crea un nuevo método de pago para el alojamiento del propietario
     * y envía un correo electrónico de confirmación.
     *
     * <p><strong>Flujo del proceso:</strong></p>
     * <ol>
     *   <li>Valida que paymentType y displayName sean obligatorios.</li>
     *   <li>Obtiene el alojamiento del propietario.</li>
     *   <li>Si se marca como predeterminado, limpia el flag de los demás.</li>
     *   <li>Si no hay método predeterminado, el primero se establece como tal.</li>
     *   <li>Persiste el nuevo método de pago.</li>
     *   <li>🆕 Envía email de confirmación al propietario.</li>
     * </ol>
     *
     * @param ownerId el identificador único del propietario autenticado
     * @param request los datos del método de pago a crear
     * @return el método de pago creado
     * @throws BusinessException si faltan datos obligatorios
     */
    @Transactional
    public AccommodationPaymentMethod create(Long ownerId, AccommodationPaymentMethodRequest request) {
        // 1. Validación de campos obligatorios
        if (request.getPaymentType() == null || request.getDisplayName() == null
                || request.getDisplayName().isBlank()) {
            throw new BusinessException("paymentType y displayName son obligatorios.");
        }

        // 2. Obtener el alojamiento del propietario
        Accommodation accommodation = getAccommodation(ownerId);

        // 3. Determinar si debe ser el método predeterminado
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || !paymentMethodRepository.existsByAccommodation_IdAndDefaultMethodTrue(
                accommodation.getId());

        // 4. Si será predeterminado, limpiar el flag de los demás métodos
        if (makeDefault) {
            paymentMethodRepository.clearDefaultForAccommodation(accommodation.getId());
        }

        // 5. Construir y persistir el nuevo método de pago
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.repository.AccommodationPaymentMethodRepository;
import com.hostflow.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationPaymentMethodService {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationPaymentMethodRepository paymentMethodRepository;

    @Transactional(readOnly = true)
    public List<AccommodationPaymentMethod> list(Long ownerId) {
        Accommodation accommodation = getAccommodation(ownerId);
        return paymentMethodRepository.findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(accommodation.getId());
    }

    @Transactional
    public AccommodationPaymentMethod create(Long ownerId, AccommodationPaymentMethodRequest request) {
        if (request.getPaymentType() == null || request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new BusinessException("paymentType y displayName son obligatorios.");
        }
        Accommodation accommodation = getAccommodation(ownerId);
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || !paymentMethodRepository.existsByAccommodation_IdAndDefaultMethodTrue(accommodation.getId());
        if (makeDefault) paymentMethodRepository.clearDefaultForAccommodation(accommodation.getId());

        AccommodationPaymentMethod method = new AccommodationPaymentMethod();
        method.setAccommodation(accommodation);
        method.setPaymentType(request.getPaymentType());
        method.setDisplayName(request.getDisplayName().trim());
        applyOptionalFields(method, request);
        method.setDefault(makeDefault);
        method.setActive(request.getIsActive() == null || request.getIsActive());

        AccommodationPaymentMethod saved = paymentMethodRepository.save(method);

        log.info("💳 Método de pago creado: '{}' ({}) para alojamiento '{}' (ID: {})",
                saved.getDisplayName(), saved.getPaymentType(),
                accommodation.getName(), accommodation.getId());

        // 6. 🆕 Enviar email de confirmación al propietario
        sendPaymentMethodCreatedEmail(accommodation, saved);

        return saved;
    }

    // =========================================================================
    // ACTUALIZAR MÉTODO DE PAGO
    // =========================================================================

    /**
     * Actualiza parcialmente un método de pago existente.
     *
     * <p>Solo modifica los campos enviados en la solicitud (PATCH).
     * Los campos omitidos o null permanecen sin cambios.</p>
     *
     * @param ownerId  el identificador único del propietario autenticado
     * @param methodId el identificador del método de pago a actualizar
     * @param request  los datos a actualizar (solo campos no nulos)
     * @return el método de pago actualizado
     * @throws BusinessException si la solicitud está vacía o displayName es vacío
     * @throws ResourceNotFoundException si el método no existe o no pertenece al alojamiento
     */
    /*@Transactional
    public AccommodationPaymentMethod update(Long ownerId, Long methodId,
                                             AccommodationPaymentMethodRequest request) {
        if (isEmptyPatch(request)) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar.");
        }

        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository
                .findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ese método de pago en tu alojamiento."));

        if (request.getDisplayName() != null && request.getDisplayName().isBlank()) {
            throw new BusinessException("displayName no puede estar vacío.");
        }*/

        validateRequiredFieldsByType(method);
        return paymentMethodRepository.save(method);
    }

    @Transactional
    public AccommodationPaymentMethod update(Long ownerId, Long methodId, AccommodationPaymentMethodRequest request) {
        if (isEmptyPatch(request)) throw new BusinessException("Debe enviar al menos un campo para actualizar.");
        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository.findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ese método de pago en tu alojamiento."));
        if (request.getDisplayName() != null && request.getDisplayName().isBlank()) {
            throw new BusinessException("displayName no puede estar vacío.");
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            paymentMethodRepository.clearDefaultForAccommodation(accommodation.getId());
            method.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            method.setDefault(false);
        }
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
        if (request.getInstructions() != null) method.setInstructions(blankToNull(request.getInstructions()));
        if (request.getIsActive() != null) method.setActive(request.getIsActive());

        return paymentMethodRepository.save(method);
    }

    // =========================================================================
    // ELIMINAR MÉTODO DE PAGO
    // =========================================================================

    /**
     * Elimina un método de pago del alojamiento.
     *
     * <p>Si el método eliminado era el predeterminado, se asigna automáticamente
     * el método más antiguo restante como nuevo predeterminado.</p>
     *
     * @param ownerId  el identificador único del propietario autenticado
     * @param methodId el identificador del método de pago a eliminar
     * @throws ResourceNotFoundException si el método no existe o no pertenece al alojamiento
     */
    /*@Transactional
    public void delete(Long ownerId, Long methodId) {
        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository
                .findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ese método de pago en tu alojamiento."));

        boolean wasDefault = method.isDefault();
        paymentMethodRepository.delete(method);

        // Si era el predeterminado, asignar otro automáticamente
        if (wasDefault) {
            List<AccommodationPaymentMethod> remaining = paymentMethodRepository
                    .findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(
                            accommodation.getId());*/
        if (request.getUserIdentifier() != null) method.setUserIdentifier(blankToNull(request.getUserIdentifier()));
        if (request.getWalletPhone() != null) method.setWalletPhone(blankToNull(request.getWalletPhone()));
        if (request.getWalletEmail() != null) method.setWalletEmail(blankToNull(request.getWalletEmail()));
        if (request.getInstructions() != null) method.setInstructions(blankToNull(request.getInstructions()));
        if (request.getIsActive() != null) method.setActive(request.getIsActive());
        validateRequiredFieldsByType(method);
        return paymentMethodRepository.save(method);
    }

    @Transactional
    public void delete(Long ownerId, Long methodId) {
        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository.findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ese método de pago en tu alojamiento."));
        boolean wasDefault = method.isDefault();
        paymentMethodRepository.delete(method);
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
    // 🆕 MÉTODO PRIVADO: ENVÍO DE EMAIL
    // =========================================================================

    /**
     * 🆕 Envía el correo electrónico de confirmación al propietario
     * cuando se crea un nuevo método de pago.
     *
     * <p>Extrae la información del alojamiento y del propietario para
     * construir un correo personalizado con todos los detalles.</p>
     *
     * @param accommodation el alojamiento donde se registró el método
     * @param method        el método de pago recién creado
     */
    private void sendPaymentMethodCreatedEmail(Accommodation accommodation,
                                               AccommodationPaymentMethod method) {
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
            // Si falla el envío del email, solo registramos el error en los logs
            // pero NO revertimos la creación del método de pago
            log.error("⚠️ No se pudo enviar el email de confirmación de método de pago: {}",
                    e.getMessage(), e);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Obtiene el alojamiento asociado al propietario o lanza una excepción si no existe.
     *
     * @param ownerId el identificador único del propietario
     * @return la entidad Accommodation asociada
     * @throws ResourceNotFoundException si el propietario no tiene alojamiento registrado
     */
    /*private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento."));
    }

   
    private boolean isEmptyPatch(AccommodationPaymentMethodRequest r) {
        return r.getPaymentType() == null && r.getDisplayName() == null
                && r.getProviderName() == null && r.getBankName() == null
                && r.getAccountHolder() == null && r.getAccountNumber() == null
                && r.getCbuCvu() == null && r.getAlias() == null
                && r.getTaxId() == null && r.getInstructions() == null
                && r.getIsDefault() == null && r.getIsActive() == null;
    }

    
    private void applyOptionalFields(AccommodationPaymentMethod m,
                                     AccommodationPaymentMethodRequest r) {
        m.setProviderName(blankToNull(r.getProviderName()));
        m.setBankName(blankToNull(r.getBankName()));
        m.setAccountHolder(blankToNull(r.getAccountHolder()));*/
            }
        }
    }

    private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Registra primero los datos generales del alojamiento."));
    }

    private boolean isEmptyPatch(AccommodationPaymentMethodRequest r) {
        return r.getPaymentType() == null && r.getDisplayName() == null && r.getProviderName() == null
                && r.getBankName() == null && r.getAccountHolder() == null && r.getAccountType() == null
                && r.getAccountNumber() == null && r.getCbuCvu() == null && r.getAlias() == null && r.getTaxId() == null
                && r.getUserIdentifier() == null && r.getWalletPhone() == null && r.getWalletEmail() == null
                && r.getInstructions() == null && r.getIsDefault() == null && r.getIsActive() == null;
    }

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
     * Convierte un string vacío o en blanco a null, o lo trimmea si tiene contenido.
     *
     * @param value el valor a procesar
     * @return null si está vacío, o el valor trimmeado
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
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

    private void requireField(String value, String fieldName, String paymentType) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " es obligatorio para " + paymentType + ".");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

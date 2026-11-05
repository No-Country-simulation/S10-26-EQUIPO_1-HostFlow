package com.hostflow.service;

import com.hostflow.dto.request.CreateGuestRequest;
import com.hostflow.dto.request.CreateReservationRequest;
import com.hostflow.dto.response.ReservationLinkResponse;
import com.hostflow.dto.response.ReservationResponse;
import com.hostflow.enums.ReservationStatus;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.*;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.ReservationLinkRepository;
import com.hostflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio central para la gestión de reservas en HostFlow.
 *
 * <p>Responsabilidades principales:</p>
 * <ul>
 *   <li>Crear reservas validando estrictamente la disponibilidad del alojamiento.</li>
 *   <li>Generar enlaces de confirmación únicos con expiración configurable.</li>
 *   <li>Confirmar reservas mediante el enlace recibido por el huésped.</li>
 *   <li>Enviar notificaciones HTML personalizadas (con branding, alergias y lógica de monedas).</li>
 *   <li>Reactivar enlaces expirados, gestionar cancelaciones y marcar reservas como completadas.</li>
 * </ul>
 *
 * <p><strong>Reglas de negocio estrictas:</strong></p>
 * <ul>
 *   <li>No se permiten reservas overlapping (doble booking) en el mismo alojamiento.</li>
 *   <li>El huésped debe tener al menos un método de pago activo registrado.</li>
 *   <li>Las fechas deben ser válidas (check-out > check-in, ambas en el futuro).</li>
 *   <li>El precio total se calcula automáticamente (noches × precio base + impuestos).</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 3.4.0 (Agregado método para completar reservas y registrar ingresos en el dashboard)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    private final ReservationRepository reservationRepository;
    private final ReservationLinkRepository linkRepository;
    private final AccommodationRepository accommodationRepository;
    private final GuestService guestService;
    private final EmailService emailService;

    // =========================================================================
    // CONFIGURACIÓN INYECTADA DESDE application.properties
    // =========================================================================

    @Value("${app.reservations.link-expiration-hours:24}")
    private int defaultLinkExpirationHours;

    @Value("${app.reservations.frontend-confirm-url:http://localhost:3000/reservations/confirm}")
    private String frontendConfirmUrl;

    @Value("${app.reservations.tax-percentage:10}")
    private BigDecimal taxPercentage;

    // =========================================================================
    // 1. CREACIÓN DE RESERVA
    // =========================================================================

    /**
     * Crea una nueva reserva y genera el enlace de confirmación.
     *
     * @param ownerId ID del propietario que crea la reserva
     * @param request datos de la reserva solicitada
     * @return la reserva creada con el enlace de confirmación incluido
     * @throws BusinessException si las fechas se superponen o el huésped no tiene métodos de pago
     */
    @Transactional
    public ReservationResponse createReservation(Long ownerId, CreateReservationRequest request) {
        log.info("🏨 Creando nueva reserva para owner ID: {}", ownerId);

        // 1. Validar que el alojamiento exista y pertenezca al owner
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró un alojamiento registrado para el propietario autenticado."));

        // 2. Validar que las fechas sean lógicas y futuras
        validateDates(request.checkIn(), request.checkOut());

        // 3. 🛡️ VALIDACIÓN CRÍTICA: Verificar disponibilidad (no overlapping)
        validateAvailability(accommodation.getId(), request.checkIn(), request.checkOut());

        // 4. 🆕 Obtener o crear el huésped (incluyendo preferencias y alergias)
        CreateGuestRequest guestRequest = new CreateGuestRequest(
                request.guestFirstName(),
                request.guestLastName(),
                request.guestEmail(),
                request.guestPhone(),
                null, // El documento de identidad no es obligatorio en este flujo
                request.guestPreferences(), // 🆕 Se mapean las preferencias del huésped
                request.guestAllergies()    // 🆕 Se mapea la lista de alergias del huésped
        );
        Guest guest = guestService.createOrFindGuest(guestRequest);

        // 5. 🛡️ VALIDACIÓN: Verificar que el huésped tenga al menos un método de pago activo
        if (!guestService.hasActivePaymentMethod(guest.getId())) {
            throw new BusinessException(
                    "El huésped debe tener al menos un método de pago activo para realizar una reserva. " +
                            "Por favor, registre un método de pago antes de continuar.");
        }

        // 6. Calcular precios
        int nights = calculateNights(request.checkIn(), request.checkOut());
        BigDecimal pricePerNight = accommodation.getBasePrice();
        BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(nights));
        BigDecimal taxes = subtotal.multiply(taxPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxes);

        // 7. Generar código de confirmación único (ej: RES-A1B2C3)
        String confirmationCode = generateConfirmationCode();

        // 8. Construir y persistir la entidad Reservation
        Reservation reservation = Reservation.builder()
                .confirmationCode(confirmationCode)
                .accommodation(accommodation)
                .guest(guest)
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .nights(nights)
                .pricePerNight(pricePerNight)
                .subtotal(subtotal)
                .taxes(taxes)
                .totalAmount(totalAmount)
                .status(ReservationStatus.PENDING)
                .guestCount(request.getGuestCountOrDefault())
                .specialRequests(request.specialRequests())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("✅ Reserva creada: {} para huésped {}", confirmationCode, guest.getEmail());

        // 9. Generar enlace de confirmación con token UUID
        ReservationLink link = createConfirmationLink(savedReservation, request.getLinkExpirationHoursOrDefault());

        // 10. 📧 Enviar email al huésped con el enlace de confirmación
        emailService.sendReservationConfirmationEmail(
                guest.getEmail(),
                guest.getFullName(),
                savedReservation.getConfirmationCode(),
                link.getConfirmationUrl(),
                accommodation.getName(),
                savedReservation.getCheckIn(),
                savedReservation.getCheckOut(),
                savedReservation.getTotalAmount(),
                request.getLinkExpirationHoursOrDefault()
        );
        log.info("📧 Email de solicitud de confirmación enviado exitosamente a: {}", guest.getEmail());

        // 11. Construir y retornar la respuesta DTO
        return ReservationResponse.from(savedReservation, ReservationLinkResponse.from(link));
    }

    // =========================================================================
    // 2. CONFIRMACIÓN DE RESERVA
    // =========================================================================

    /**
     * Confirma una reserva utilizando el enlace de confirmación recibido por el huésped.
     * Envía notificaciones HTML personalizadas tanto al huésped como al propietario.
     *
     * @param token           el token del enlace de confirmación
     * @param paymentMethodId ID del método de pago a utilizar
     * @return la reserva confirmada
     * @throws BusinessException si el enlace es inválido o el método de pago no es válido
     */
    @Transactional
    public ReservationResponse confirmReservation(String token, Long paymentMethodId) {
        log.info("🔐 Confirmando reserva con token: {}", token);

        // 1. Buscar el enlace
        ReservationLink link = linkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("El enlace de confirmación es inválido o no existe."));

        // 2. Validar que el enlace sea válido (no usado y no expirado)
        if (Boolean.TRUE.equals(link.getUsed())) {
            throw new BusinessException("Este enlace de confirmación ya fue utilizado.");
        }
        if (link.isExpired()) {
            throw new BusinessException("Este enlace de confirmación ha expirado. Por favor, solicite un nuevo enlace al propietario.");
        }

        // 3. Obtener la reserva asociada
        Reservation reservation = link.getReservation();

        // 4. Validar que la reserva esté en estado PENDING
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("La reserva ya no está pendiente de confirmación. Estado actual: " + reservation.getStatus().getDisplayName());
        }

        // 5. 🛡️ VALIDACIÓN: Verificar que el método de pago sea válido y pertenezca al huésped
        GuestPaymentMethod paymentMethod = guestService.getPaymentMethod(reservation.getGuest().getId(), paymentMethodId);
        if (!paymentMethod.isActive()) {
            throw new BusinessException("El método de pago seleccionado no está activo. Por favor, seleccione otro método de pago.");
        }

        // 6. 🛡️ RE-VALIDACIÓN: Verificar disponibilidad nuevamente (prevención de race conditions)
        validateAvailability(reservation.getAccommodation().getId(), reservation.getCheckIn(), reservation.getCheckOut(), reservation.getId());

        // 7. Confirmar la reserva y asociar el método de pago
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentMethod(paymentMethod);
        Reservation confirmedReservation = reservationRepository.save(reservation);

        // 8. Marcar el enlace como usado
        link.markAsUsed();
        linkRepository.save(link);

        log.info("✅ Reserva confirmada en BD: {} - Huésped: {}", confirmedReservation.getConfirmationCode(), confirmedReservation.getGuest().getEmail());

        // =========================================================================
        // 9. 📧 ENVÍO DE NOTIFICACIONES HTML PERSONALIZADAS (Con política de resiliencia)
        // =========================================================================
        try {
            log.info("📧 Iniciando envío de emails de confirmación para la reserva: {}", confirmedReservation.getConfirmationCode());

            // Carga explícita del alojamiento para evitar LazyInitializationException
            Accommodation accommodation = accommodationRepository.findById(confirmedReservation.getAccommodation().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado para la reserva confirmada."));

            String guestName = confirmedReservation.getGuest().getFullName();

            // Lógica segura para obtener el nombre del propietario
            String ownerName = "Propietario";
            if (accommodation.getOwner() != null) {
                if (accommodation.getOwner().getFullName() != null && !accommodation.getOwner().getFullName().isBlank()) {
                    ownerName = accommodation.getOwner().getFullName();
                } else if (accommodation.getOwner().getEmail() != null && !accommodation.getOwner().getEmail().isBlank()) {
                    ownerName = accommodation.getOwner().getEmail().split("@")[0];
                }
            }

            // Construir URL base para que puedan ver la reserva en la plataforma
            String baseUrl = frontendConfirmUrl.replace("/confirm", "");
            String reservationUrl = baseUrl + "/reservations/" + confirmedReservation.getConfirmationCode();

            // A. Enviar email HTML al Huésped
            emailService.sendReservationConfirmedGuestEmail(confirmedReservation, accommodation, guestName, reservationUrl);

            // B. Enviar email HTML al Propietario
            emailService.sendReservationConfirmedOwnerEmail(confirmedReservation, accommodation, ownerName, reservationUrl);

            log.info("📧 Emails de confirmación (HTML) enviados exitosamente al huésped y al propietario.");

        } catch (Exception e) {
            // POLÍTICA DE RESILIENCIA: No lanzar excepción para no revertir la transacción si el servidor de correo falla.
            log.error("❌ Error al enviar emails de confirmación HTML (Reserva {}): {}", confirmedReservation.getConfirmationCode(), e.getMessage(), e);
        }

        return ReservationResponse.from(confirmedReservation, null);
    }

    // =========================================================================
    // 3. REACTIVACIÓN DE ENLACE
    // =========================================================================

    /**
     * Reactiva un enlace de confirmación expirado.
     *
     * @param reservationId   ID de la reserva
     * @param expirationHours horas de validez del nuevo enlace
     * @return el enlace reactivado
     * @throws BusinessException si la reserva ya está confirmada o cancelada
     */
    @Transactional
    public ReservationLinkResponse reactivateLink(Long reservationId, int expirationHours) {
        log.info("🔄 Reactivando enlace para reserva ID: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Solo se pueden reactivar enlaces de reservas pendientes. Estado actual: " + reservation.getStatus().getDisplayName());
        }

        ReservationLink link = linkRepository.findByReservation_Id(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un enlace para esta reserva."));

        String newToken = link.reactivate(expirationHours);
        String newUrl = frontendConfirmUrl + "?token=" + newToken;
        link.setConfirmationUrl(newUrl);
        linkRepository.save(link);

        log.info("✅ Enlace reactivado. Nuevo token: {}", newToken);

        emailService.sendReservationConfirmationEmail(
                reservation.getGuest().getEmail(),
                reservation.getGuest().getFullName(),
                reservation.getConfirmationCode(),
                link.getConfirmationUrl(),
                reservation.getAccommodation().getName(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getTotalAmount(),
                expirationHours
        );
        log.info("📧 Email con nuevo enlace de confirmación enviado a: {}", reservation.getGuest().getEmail());

        return ReservationLinkResponse.from(link);
    }

    // =========================================================================
    // 4. CANCELACIÓN DE RESERVA
    // =========================================================================

    /**
     * Cancela una reserva existente.
     *
     * @param reservationId ID de la reserva
     * @param reason        motivo de la cancelación
     * @return la reserva cancelada
     */
    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, String reason) {
        log.info("❌ Cancelando reserva ID: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("La reserva ya está cancelada.");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException("No se puede cancelar una reserva que ya ha sido completada.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(reason);
        reservation.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("✅ Reserva cancelada: {}", reservation.getConfirmationCode());

        emailService.sendReservationCancelledEmail(
                reservation.getGuest().getEmail(),
                reservation.getGuest().getFullName(),
                reservation.getConfirmationCode(),
                reservation.getAccommodation().getName(),
                reason
        );
        log.info("📧 Email de cancelación enviado exitosamente a: {}", reservation.getGuest().getEmail());

        return ReservationResponse.from(reservation, null);
    }

    // =========================================================================
    // 5. 🆕 COMPLETAR RESERVA (REGISTRO DE INGRESOS)
    // =========================================================================

    /**
     * Marca una reserva como COMPLETADA después del check-out.
     *
     * <p>Esta operación es irreversible y actualiza las métricas financieras
     * del propietario (incomeLastMonth, accumulatedIncome) en el dashboard.</p>
     *
     * @param reservationId ID de la reserva a completar
     * @return la reserva actualizada
     * @throws BusinessException si la reserva no está en estado CONFIRMED o el check-out no ha pasado
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional
    public ReservationResponse completeReservation(Long reservationId) {
        log.info("✅ Completando reserva ID: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        // Solo se pueden completar reservas CONFIRMED
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                    "Solo se pueden completar reservas en estado CONFIRMED. " +
                            "Estado actual: " + reservation.getStatus().getDisplayName());
        }

        // Validar que la fecha de check-out ya haya pasado
        if (reservation.getCheckOut().isAfter(LocalDateTime.now())) {
            throw new BusinessException(
                    "No se puede completar una reserva cuya fecha de check-out " +
                            "aún no ha llegado. Check-out: " + reservation.getCheckOut());
        }

        // Cambiar estado a COMPLETED
        reservation.setStatus(ReservationStatus.COMPLETED);
        Reservation completed = reservationRepository.save(reservation);

        log.info("✅ Reserva completada: {} | Ingreso registrado: ${}",
                completed.getConfirmationCode(), completed.getTotalAmount());

        return ReservationResponse.from(completed, null);
    }

    // =========================================================================
    // 6. CONSULTAS
    // =========================================================================

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        ReservationLinkResponse linkResponse = linkRepository.findByReservation_Id(reservationId)
                .map(ReservationLinkResponse::from)
                .orElse(null);

        return ReservationResponse.from(reservation, linkResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationByConfirmationCode(String confirmationCode) {
        Reservation reservation = reservationRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con código: " + confirmationCode));

        ReservationLinkResponse linkResponse = linkRepository.findByReservation_Id(reservation.getId())
                .map(ReservationLinkResponse::from)
                .orElse(null);

        return ReservationResponse.from(reservation, linkResponse);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservationsByAccommodation(Long accommodationId) {
        return reservationRepository.findAllByAccommodation_IdOrderByCheckInDesc(accommodationId)
                .stream()
                .map(r -> {
                    ReservationLinkResponse link = linkRepository.findByReservation_Id(r.getId())
                            .map(ReservationLinkResponse::from)
                            .orElse(null);
                    return ReservationResponse.from(r, link);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservationsByGuest(Long guestId) {
        return reservationRepository.findAllByGuest_IdOrderByCheckInDesc(guestId)
                .stream()
                .map(r -> {
                    ReservationLinkResponse link = linkRepository.findByReservation_Id(r.getId())
                            .map(ReservationLinkResponse::from)
                            .orElse(null);
                    return ReservationResponse.from(r, link);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isAccommodationAvailable(Long accommodationId, LocalDateTime checkIn, LocalDateTime checkOut) {
        return !reservationRepository.existsOverlappingReservation(accommodationId, checkIn, checkOut);
    }

    // =========================================================================
    // 7. MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Valida que las fechas de check-in y check-out sean lógicas y futuras.
     */
    private void validateDates(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn.isBefore(LocalDateTime.now())) {
            throw new BusinessException("La fecha de check-in no puede ser en el pasado.");
        }
        if (checkOut.isBefore(LocalDateTime.now())) {
            throw new BusinessException("La fecha de check-out no puede ser en el pasado.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException("La fecha de check-out debe ser posterior a la fecha de check-in.");
        }

        int nights = calculateNights(checkIn, checkOut);
        if (nights < 1) {
            throw new BusinessException("La estadía mínima es de 1 noche.");
        }
        if (nights > 365) {
            throw new BusinessException("La estadía máxima es de 365 noches.");
        }
    }

    /**
     * 🛡️ VALIDACIÓN CRÍTICA: Verifica que no haya reservas overlapping.
     */
    private void validateAvailability(Long accommodationId, LocalDateTime checkIn, LocalDateTime checkOut) {
        validateAvailability(accommodationId, checkIn, checkOut, null);
    }

    /**
     * 🛡️ VALIDACIÓN CRÍTICA: Verifica que no haya reservas overlapping,
     * excluyendo una reserva específica (útil al re-confirmar).
     */
    private void validateAvailability(Long accommodationId, LocalDateTime checkIn, LocalDateTime checkOut, Long excludeReservationId) {
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(accommodationId, checkIn, checkOut);

        // Filtrar la reserva a excluir si se proporciona (para evitar falsos positivos al re-validar)
        if (excludeReservationId != null) {
            overlapping = overlapping.stream()
                    .filter(r -> !r.getId().equals(excludeReservationId))
                    .collect(Collectors.toList());
        }

        if (!overlapping.isEmpty()) {
            log.warn("⚠️ Conflicto de disponibilidad detectado para alojamiento ID: {}", accommodationId);
            throw new BusinessException("El alojamiento no está disponible en las fechas solicitadas. Ya existe una reserva activa en ese período.");
        }
    }

    /**
     * Calcula el número de noches entre check-in y check-out.
     */
    private int calculateNights(LocalDateTime checkIn, LocalDateTime checkOut) {
        return (int) Duration.between(checkIn, checkOut).toDays();
    }

    /**
     * Genera un código de confirmación único.
     * Formato: RES-XXXXXX (6 caracteres alfanuméricos aleatorios)
     */
    private String generateConfirmationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder("RES-");
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    /**
     * Crea un enlace de confirmación para una reserva.
     */
    private ReservationLink createConfirmationLink(Reservation reservation, int expirationHours) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expirationHours);
        String confirmationUrl = frontendConfirmUrl + "?token=" + token;

        ReservationLink link = ReservationLink.builder()
                .reservation(reservation)
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .reactivationCount(0)
                .confirmationUrl(confirmationUrl)
                .build();

        return linkRepository.save(link);
    }
}
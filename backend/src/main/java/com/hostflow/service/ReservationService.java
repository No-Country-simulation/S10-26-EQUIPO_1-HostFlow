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
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.ReservationLinkRepository;
import com.hostflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationLinkRepository linkRepository;
    private final AccommodationRepository accommodationRepository;
    private final OwnerRepository ownerRepository;
    private final GuestService guestService;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.reservations.link-expiration-hours:24}")
    private int defaultLinkExpirationHours;

    @Value("${app.reservations.frontend-confirm-url:http://localhost:3000/reservations/confirm}")
    private String frontendConfirmUrl;

    @Value("${app.reservations.tax-percentage:10}")
    private BigDecimal taxPercentage;

    // =========================================================================
    // MÉTODOS PÚBLICOS QUE ACEPTAN EMAIL
    // =========================================================================

    @Transactional
    public ReservationResponse createReservationByEmail(String ownerEmail, CreateReservationRequest request) {
        Long ownerId = getAccommodationByEmail(ownerEmail).getOwner().getId();
        return createReservation(ownerId, request);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listMyReservationsByEmail(String ownerEmail) {
        Long accommodationId = getAccommodationByEmail(ownerEmail).getId();
        return listReservationsByAccommodation(accommodationId);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationByIdAndOwner(Long reservationId, String ownerEmail) {
        Long accommodationId = getAccommodationByEmail(ownerEmail).getId();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        if (!reservation.getAccommodation().getId().equals(accommodationId)) {
            throw new ResourceNotFoundException("La reserva no pertenece a tu alojamiento.");
        }

        ReservationLinkResponse linkResponse = linkRepository.findByReservation_Id(reservationId)
                .map(ReservationLinkResponse::from)
                .orElse(null);

        return ReservationResponse.from(reservation, linkResponse);
    }

    @Transactional
    public ReservationResponse cancelReservationByIdAndOwner(Long reservationId, String reason, String ownerEmail) {
        Long accommodationId = getAccommodationByEmail(ownerEmail).getId();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        if (!reservation.getAccommodation().getId().equals(accommodationId)) {
            throw new ResourceNotFoundException("La reserva no pertenece a tu alojamiento.");
        }

        return cancelReservation(reservationId, reason);
    }

    @Transactional
    public ReservationLinkResponse reactivateLinkByIdAndOwner(Long reservationId, int expirationHours, String ownerEmail) {
        Long accommodationId = getAccommodationByEmail(ownerEmail).getId();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        if (!reservation.getAccommodation().getId().equals(accommodationId)) {
            throw new ResourceNotFoundException("La reserva no pertenece a tu alojamiento.");
        }

        return reactivateLink(reservationId, expirationHours);
    }

    @Transactional(readOnly = true)
    public boolean checkAvailabilityByEmail(String ownerEmail, LocalDateTime checkIn, LocalDateTime checkOut) {
        Long accommodationId = getAccommodationByEmail(ownerEmail).getId();
        return isAccommodationAvailable(accommodationId, checkIn, checkOut);
    }

    @Transactional
    public ReservationResponse completeReservationByIdAndOwner(Long reservationId, String ownerEmail) {
        Long accommodationId = getAccommodationByEmail(ownerEmail).getId();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId));

        if (!reservation.getAccommodation().getId().equals(accommodationId)) {
            throw new ResourceNotFoundException("La reserva no pertenece a tu alojamiento.");
        }

        return completeReservation(reservationId);
    }

    // =========================================================================
    // MÉTODOS CENTRALES DE LÓGICA DE NEGOCIO
    // =========================================================================

    @Transactional
    public ReservationResponse createReservation(Long ownerId, CreateReservationRequest request) {
        log.info("🏨 Creando nueva reserva para owner ID: {}", ownerId);

        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un alojamiento registrado para el propietario autenticado."));

        validateDates(request.checkIn(), request.checkOut());
        validateAvailability(accommodation.getId(), request.checkIn(), request.checkOut());

        CreateGuestRequest guestRequest = new CreateGuestRequest(
                request.guestFirstName(),
                request.guestLastName(),
                request.guestEmail(),
                request.guestPhone(),
                null,
                request.guestPreferences(),
                request.guestAllergies()
        );
        Guest guest = guestService.createOrFindGuest(guestRequest);

        if (!guestService.hasActivePaymentMethod(guest.getId())) {
            throw new BusinessException("El huésped debe tener al menos un método de pago activo para realizar una reserva.");
        }

        int nights = calculateNights(request.checkIn(), request.checkOut());
        BigDecimal pricePerNight = accommodation.getBasePrice();
        BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(nights));
        BigDecimal taxes = subtotal.multiply(taxPercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxes);

        String confirmationCode = generateConfirmationCode();

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

        ReservationLink link = createConfirmationLink(savedReservation, request.getLinkExpirationHoursOrDefault());

        emailService.sendReservationConfirmationEmail(
                guest.getEmail(), guest.getFullName(), savedReservation.getConfirmationCode(),
                link.getConfirmationUrl(), accommodation.getName(), savedReservation.getCheckIn(),
                savedReservation.getCheckOut(), savedReservation.getTotalAmount(),
                request.getLinkExpirationHoursOrDefault()
        );

        return ReservationResponse.from(savedReservation, ReservationLinkResponse.from(link));
    }

    @Transactional
    public ReservationResponse confirmReservation(String token, Long paymentMethodId) {
        log.info("🔐 Confirmando reserva con token: {}", token);

        ReservationLink link = linkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("El enlace de confirmación es inválido o no existe."));

        if (Boolean.TRUE.equals(link.getUsed())) {
            throw new BusinessException("Este enlace de confirmación ya fue utilizado.");
        }
        if (link.isExpired()) {
            throw new BusinessException("Este enlace de confirmación ha expirado.");
        }

        Reservation reservation = link.getReservation();

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("La reserva ya no está pendiente de confirmación.");
        }

        GuestPaymentMethod paymentMethod = guestService.getPaymentMethodEntity(reservation.getGuest().getId(), paymentMethodId);
        if (!paymentMethod.isActive()) {
            throw new BusinessException("El método de pago seleccionado no está activo.");
        }

        validateAvailability(reservation.getAccommodation().getId(), reservation.getCheckIn(), reservation.getCheckOut(), reservation.getId());

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentMethod(paymentMethod);
        Reservation confirmedReservation = reservationRepository.save(reservation);

        link.markAsUsed();
        linkRepository.save(link);

        try {
            Accommodation accommodation = accommodationRepository.findById(confirmedReservation.getAccommodation().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado."));

            String ownerName = "Propietario";
            if (accommodation.getOwner() != null && accommodation.getOwner().getFullName() != null) {
                ownerName = accommodation.getOwner().getFullName();
            }

            String baseUrl = frontendConfirmUrl.replace("/confirm", "");
            String reservationUrl = baseUrl + "/reservations/" + confirmedReservation.getConfirmationCode();

            emailService.sendReservationConfirmedGuestEmail(confirmedReservation, accommodation, confirmedReservation.getGuest().getFullName(), reservationUrl);
            emailService.sendReservationConfirmedOwnerEmail(confirmedReservation, accommodation, ownerName, reservationUrl);

        } catch (Exception e) {
            log.error("❌ Error al enviar emails de confirmación HTML: {}", e.getMessage());
        }

        return ReservationResponse.from(confirmedReservation, null);
    }

    @Transactional
    public ReservationLinkResponse reactivateLink(Long reservationId, int expirationHours) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva."));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Solo se pueden reactivar enlaces de reservas pendientes.");
        }

        ReservationLink link = linkRepository.findByReservation_Id(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un enlace para esta reserva."));

        String newToken = link.reactivate(expirationHours);
        link.setConfirmationUrl(frontendConfirmUrl + "?token=" + newToken);
        linkRepository.save(link);

        emailService.sendReservationConfirmationEmail(
                reservation.getGuest().getEmail(), reservation.getGuest().getFullName(),
                reservation.getConfirmationCode(), link.getConfirmationUrl(),
                reservation.getAccommodation().getName(), reservation.getCheckIn(),
                reservation.getCheckOut(), reservation.getTotalAmount(), expirationHours
        );

        return ReservationLinkResponse.from(link);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva."));

        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException("No se puede cancelar esta reserva.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason(reason);
        reservation.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        emailService.sendReservationCancelledEmail(
                reservation.getGuest().getEmail(), reservation.getGuest().getFullName(),
                reservation.getConfirmationCode(), reservation.getAccommodation().getName(), reason
        );

        return ReservationResponse.from(reservation, null);
    }

    @Transactional
    public ReservationResponse completeReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva."));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException("Solo se pueden completar reservas en estado CONFIRMED.");
        }
        if (reservation.getCheckOut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("La fecha de check-out aún no ha llegado.");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        return ReservationResponse.from(reservationRepository.save(reservation), null);
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva."));
        ReservationLinkResponse link = linkRepository.findByReservation_Id(reservationId).map(ReservationLinkResponse::from).orElse(null);
        return ReservationResponse.from(reservation, link);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationByConfirmationCode(String confirmationCode) {
        Reservation reservation = reservationRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva."));
        ReservationLinkResponse link = linkRepository.findByReservation_Id(reservation.getId()).map(ReservationLinkResponse::from).orElse(null);
        return ReservationResponse.from(reservation, link);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservationsByAccommodation(Long accommodationId) {
        // ✅ CORRECCIÓN DEFINITIVA: Llama al método exacto que existe en el Repository
        return reservationRepository.findAllByAccommodation_IdOrderByCheckInDesc(accommodationId)
                .stream()
                .map(r -> ReservationResponse.from(r, linkRepository.findByReservation_Id(r.getId()).map(ReservationLinkResponse::from).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservationsByGuest(Long guestId) {
        // ✅ CORRECCIÓN DEFINITIVA: Llama al método exacto que existe en el Repository
        return reservationRepository.findAllByGuest_IdOrderByCheckInDesc(guestId)
                .stream()
                .map(r -> ReservationResponse.from(r, linkRepository.findByReservation_Id(r.getId()).map(ReservationLinkResponse::from).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isAccommodationAvailable(Long accommodationId, LocalDateTime checkIn, LocalDateTime checkOut) {
        return !reservationRepository.existsOverlappingReservation(accommodationId, checkIn, checkOut);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private Accommodation getAccommodationByEmail(String ownerEmail) {
        Owner owner = ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el propietario."));
        return accommodationRepository.findByOwner_Id(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Registra primero los datos del alojamiento."));
    }

    private void validateDates(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn.isBefore(LocalDateTime.now())) throw new BusinessException("La fecha de check-in no puede ser en el pasado.");
        if (checkOut.isBefore(LocalDateTime.now())) throw new BusinessException("La fecha de check-out no puede ser en el pasado.");
        if (!checkOut.isAfter(checkIn)) throw new BusinessException("El check-out debe ser posterior al check-in.");
        int nights = calculateNights(checkIn, checkOut);
        if (nights < 1) throw new BusinessException("La estadía mínima es de 1 noche.");
        if (nights > 365) throw new BusinessException("La estadía máxima es de 365 noches.");
    }

    private void validateAvailability(Long accommodationId, LocalDateTime checkIn, LocalDateTime checkOut) {
        validateAvailability(accommodationId, checkIn, checkOut, null);
    }

    private void validateAvailability(Long accommodationId, LocalDateTime checkIn, LocalDateTime checkOut, Long excludeReservationId) {
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(accommodationId, checkIn, checkOut);
        if (excludeReservationId != null) {
            overlapping = overlapping.stream().filter(r -> !r.getId().equals(excludeReservationId)).toList();
        }
        if (!overlapping.isEmpty()) {
            throw new BusinessException("El alojamiento no está disponible en las fechas solicitadas.");
        }
    }

    private int calculateNights(LocalDateTime checkIn, LocalDateTime checkOut) {
        return (int) Duration.between(checkIn, checkOut).toDays();
    }

    private String generateConfirmationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder("RES-");
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }

    private ReservationLink createConfirmationLink(Reservation reservation, int expirationHours) {
        String token = UUID.randomUUID().toString();
        ReservationLink link = ReservationLink.builder()
                .reservation(reservation)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(expirationHours))
                .used(false)
                .reactivationCount(0)
                .confirmationUrl(frontendConfirmUrl + "?token=" + token)
                .build();
        return linkRepository.save(link);
    }
}
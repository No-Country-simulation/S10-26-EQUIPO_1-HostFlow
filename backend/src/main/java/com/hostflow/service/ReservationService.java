package com.hostflow.service;

import com.hostflow.dto.request.CreateGuestRequest;
import com.hostflow.dto.request.CreateReservationRequest;
import com.hostflow.dto.request.CompleteReservationOnboardingRequest;
import com.hostflow.dto.response.ReservationOnboardingDetailsResponse;
import com.hostflow.dto.response.ReservationOnboardingConfirmationResponse;
import com.hostflow.dto.response.ReservationLinkResponse;
import com.hostflow.dto.response.ReservationResponse;
import com.hostflow.enums.ReservationStatus;
import com.hostflow.enums.PaymentStatus;
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.enums.Currency;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.*;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.AdditionalServiceRepository;
import com.hostflow.repository.GuestRepository;
import com.hostflow.repository.ReservationLinkRepository;
import com.hostflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
    private final AdditionalServiceRepository additionalServiceRepository;
    private final GuestRepository guestRepository;
    private final EmailService emailService;
    private final CurrencyConversionService currencyConversionService;

    // =========================================================================
    // CONFIGURACIÓN INYECTADA DESDE application.properties
    // =========================================================================


    @Value("${app.reservations.frontend-confirm-url:http://localhost:3000/reservations/confirm}")
    private String frontendConfirmUrl;


    // =========================================================================
    // 1. CREACIÓN DE RESERVA
    // =========================================================================

    /**
     * Crea una nueva reserva y genera el enlace de confirmación.
     *
     * @param ownerId ID del propietario que crea la reserva
     * @param request datos de la reserva solicitada
     * @return la reserva creada con el enlace de confirmación incluido
     * @throws BusinessException si las fechas se superponen, el alojamiento no está listo o falla el correo
     */
    @Transactional
    public ReservationResponse createReservation(Long ownerId, CreateReservationRequest request) {
        log.info("Creando reserva para propietario ID {}", ownerId);

        // Serializa las creaciones de reservas del mismo alojamiento para reducir carreras.
        Accommodation accommodation = accommodationRepository.findByOwner_IdForUpdate(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró un alojamiento registrado para el propietario autenticado."));

        validateAccommodationReadyForReservations(accommodation);
        validateDates(request.checkIn(), request.checkOut());

        int guestCount = request.getGuestCountOrDefault();
        if (guestCount > accommodation.getGuestCapacity()) {
            throw new BusinessException("La cantidad de huéspedes supera la capacidad del alojamiento (" +
                    accommodation.getGuestCapacity() + ").");
        }
        validateAvailability(accommodation.getId(), request.checkIn(), request.checkOut());

        int nights = calculateNights(request.checkIn(), request.checkOut());
        BigDecimal pricePerNight = accommodation.getBasePrice();
        BigDecimal subtotal = pricePerNight.multiply(BigDecimal.valueOf(nights));
        String guestName = (request.guestFirstName().trim() + " " + request.guestLastName().trim()).trim();
        String guestEmail = request.guestEmail().trim().toLowerCase();

        Reservation reservation = Reservation.builder()
                .confirmationCode(generateConfirmationCode())
                .accommodation(accommodation)
                .guest(null)
                .guestName(guestName)
                .guestEmail(guestEmail)
                .guestPhone(request.guestPhone() == null ? null : request.guestPhone().trim())
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .nights(nights)
                .pricePerNight(pricePerNight)
                .subtotal(subtotal)
                .totalAmount(subtotal)
                .currency(accommodation.getBaseCurrency())
                .additionalServicesTotal(BigDecimal.ZERO)
                .paymentStatus(PaymentStatus.PENDING)
                .status(ReservationStatus.PENDING)
                .guestCount(guestCount)
                .specialRequests(request.specialRequests())
                .boatTransferRequested(false)
                .build();

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        ReservationLink link = createConfirmationLink(savedReservation, 24);
        savedReservation.addLink(link);

        // Modo estricto: si el proveedor rechaza el correo, EmailDeliveryException revierte la transacción
        // y no queda una reserva sin enlace entregado al huésped.
        emailService.sendReservationConfirmationEmail(
                savedReservation.getGuestEmail(), savedReservation.getGuestName(),
                savedReservation.getConfirmationCode(), link.getConfirmationUrl(),
                accommodation, accommodation.getName(), savedReservation.getCheckIn(), savedReservation.getCheckOut(),
                savedReservation.getTotalAmount(), 24);

        log.info("Reserva {} creada; enlace de onboarding aceptado por el proveedor para {}",
                savedReservation.getConfirmationCode(), savedReservation.getGuestEmail());
        return ReservationResponse.from(savedReservation, ReservationLinkResponse.from(link));
    }

    private void validateAccommodationReadyForReservations(Accommodation a) {
        List<String> missing = new java.util.ArrayList<>();
        if (a.getName() == null || a.getName().isBlank()) missing.add("nombre del alojamiento");
        if (a.getRoomCount() == null || a.getRoomCount() < 1) missing.add("cantidad de habitaciones");
        if (a.getGuestCapacity() == null || a.getGuestCapacity() < 1) missing.add("capacidad de huéspedes");
        if (a.getBathroomCount() != null && (a.getBathroomCount() < 0 || a.getBathroomCount() > 10000)) missing.add("cantidad de baños válida");
        if (a.getBasePrice() == null || a.getBasePrice().signum() < 0) missing.add("precio base válido");
        if (a.getBaseCurrency() == null) missing.add("moneda base");
        if (a.getEmail() == null || !a.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) missing.add("correo de contacto válido");
        if (a.getPhone() == null || !a.getPhone().matches("^[+()0-9 .-]{7,40}$")) missing.add("teléfono de contacto válido");
        if (a.getAddress() == null || a.getAddress().isBlank()) missing.add("dirección");
        if (!validHexColor(a.getPrimaryColor())) missing.add("color principal válido");
        if (!validHexColor(a.getAccentColor())) missing.add("color de acento válido");
        if (!validHexColor(a.getTertiaryColor())) missing.add("color terciario válido");
        if (a.getBackgroundPattern() == null || !List.of("none", "leaves", "waves", "petals", "arches", "mountains", "scales", "sun").contains(a.getBackgroundPattern())) missing.add("patrón visual válido");
        if (a.getWelcomeMessage() == null || a.getWelcomeMessage().isBlank()) missing.add("mensaje de bienvenida");
        if (a.getLogoUrl() == null || a.getLogoUrl().isBlank()) missing.add("logotipo");
        if (a.getImages() == null || a.getImages().stream().noneMatch(i -> i.getImageUrl() != null && !i.getImageUrl().isBlank())) missing.add("al menos una imagen de galería");
        boolean validPayment = a.getPaymentMethods() != null && a.getPaymentMethods().stream().anyMatch(this::isUsablePaymentMethod);
        if (!validPayment) missing.add("al menos un método de pago activo y válido");
        if (!missing.isEmpty()) {
            throw new BusinessException("El perfil del alojamiento no habilita reservas. Requisitos pendientes: " + String.join(", ", missing) + ".");
        }
    }

    private boolean validHexColor(String color) {
        return color != null && color.matches("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    }

    private boolean isUsablePaymentMethod(AccommodationPaymentMethod m) {
        if (m == null || !m.isActive() || m.getPaymentType() == null || m.getDisplayName() == null || m.getDisplayName().isBlank()) return false;
        return switch (m.getPaymentType()) {
            case BANK_TRANSFER -> notBlank(m.getBankName()) && notBlank(m.getAccountHolder()) &&
                    (notBlank(m.getCbuCvu()) || notBlank(m.getAccountNumber()) || notBlank(m.getAlias()));
            case DIGITAL_WALLET -> notBlank(m.getProviderName()) &&
                    (notBlank(m.getUserIdentifier()) || notBlank(m.getWalletPhone()) || notBlank(m.getWalletEmail()) || notBlank(m.getAlias()));
            case CASH -> true;
            case CARD, OTHER -> notBlank(m.getInstructions()) || notBlank(m.getProviderName()) || notBlank(m.getAccountNumber());
        };
    }

    private boolean notBlank(String value) { return value != null && !value.isBlank(); }

    // =========================================================================
    // 2. CONFIRMACIÓN DE RESERVA
    // =========================================================================

    // =========================================================================
    // 3. REACTIVACIÓN DE ENLACE
    // =========================================================================

    /**
     * Renueva el enlace de onboarding con vigencia fija de 24 horas.
     *
     * <p>Invalida los enlaces vigentes, crea uno nuevo y envía el correo en modo estricto.
     * Si el envío falla, la transacción se revierte y se conservan los enlaces anteriores.</p>
     *
     * @param ownerId       ID del propietario autenticado
     * @param reservationId ID de la reserva
     * @return el nuevo enlace
     * @throws BusinessException si la reserva no está en estado PENDING o falla el correo
     */
    @Transactional
    public ReservationLinkResponse reactivateLink(Long ownerId, Long reservationId) {
        Reservation reservation = findReservationOwnedBy(ownerId, reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Solo se puede renovar el enlace de una reserva pendiente.");
        }

        linkRepository.findAllByReservation_Id(reservationId).stream()
                .filter(ReservationLink::isValid)
                .forEach(link -> { link.invalidate(); linkRepository.save(link); });

        ReservationLink newLink = createConfirmationLink(reservation, 24);
        reservation.addLink(newLink);
        // Modo estricto: si falla el envío, se revierte la renovación y se conservan los enlaces anteriores.
        emailService.sendReservationConfirmationEmail(
                reservation.getGuestEmail(), reservation.getGuestName(), reservation.getConfirmationCode(),
                newLink.getConfirmationUrl(), reservation.getAccommodation(), reservation.getAccommodation().getName(),
                reservation.getCheckIn(), reservation.getCheckOut(), reservation.getTotalAmount(), 24);
        return ReservationLinkResponse.from(newLink);
    }

    // =========================================================================
    // 3. ONBOARDING PÚBLICO DEL HUÉSPED
    // =========================================================================

    /**
     * Devuelve los datos públicos necesarios para completar el onboarding.
     * Consultar el enlace no lo consume; solo el POST exitoso marca el token como usado.
     */
    @Transactional(readOnly = true)
    public ReservationOnboardingDetailsResponse getOnboardingDetails(String token) {
        ReservationLink link = linkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el enlace de reserva."));
        validateOnboardingLink(link);

        Reservation reservation = link.getReservation();
        Accommodation accommodation = reservation.getAccommodation();

        List<ReservationOnboardingDetailsResponse.PaymentOption> paymentOptions =
                accommodation.getPaymentMethods() == null ? List.of() : accommodation.getPaymentMethods().stream()
                        .filter(this::isUsablePaymentMethod)
                        .map(this::toPaymentOption)
                        .toList();

        List<ReservationOnboardingDetailsResponse.AdditionalServiceOption> serviceOptions =
                additionalServiceRepository
                        .findAllByAccommodation_IdAndActiveTrueOrderByCreatedAtAsc(accommodation.getId())
                        .stream()
                        .map(service -> new ReservationOnboardingDetailsResponse.AdditionalServiceOption(
                                service.getId(), service.getName(), service.getDescription(),
                                service.getBasePrice(), service.getPriceType(), accommodation.getBaseCurrency()))
                        .toList();

        return new ReservationOnboardingDetailsResponse(
                reservation.getConfirmationCode(), accommodation.getName(),
                reservation.getCheckIn(), reservation.getCheckOut(), reservation.getNights(),
                reservation.getGuestCount(), reservation.getPricePerNight(), reservation.getSubtotal(),
                reservation.getAdditionalServicesTotal(), reservation.getTotalAmount(), reservation.getCurrency(),
                accommodation.getLogoUrl(), accommodation.getPrimaryColor(), accommodation.getAccentColor(),
                accommodation.getTertiaryColor(), accommodation.getBackgroundPattern(),
                accommodation.getWelcomeMessage(), paymentOptions, serviceOptions,
                buildCurrencyEquivalents(accommodation, reservation.getPricePerNight(), reservation.getSubtotal(),
                        reservation.getAdditionalServicesTotal(), reservation.getTotalAmount()));
    }

    /**
     * Completa el onboarding de forma atómica. El bloqueo pesimista del enlace
     * impide que dos solicitudes concurrentes consuman el mismo token.
     */
    @Transactional
    public ReservationOnboardingConfirmationResponse completeOnboarding(
            String token, CompleteReservationOnboardingRequest request) {
        ReservationLink link = linkRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el enlace de reserva."));
        validateOnboardingLink(link);

        Reservation reservation = link.getReservation();
        Accommodation accommodation = reservation.getAccommodation();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        String documentNumber = request.documentNumber().trim();
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        String phone = request.phone().trim();

        Guest guest = resolveGuest(firstName, lastName, email, phone, documentNumber);
        ReservationGuestInfo guestInfo = ReservationGuestInfo.builder()
                .firstName(firstName)
                .lastName(lastName)
                .documentNumber(documentNumber)
                .email(email)
                .phone(phone)
                .preferences(request.preferences() == null ? null : request.preferences().trim())
                .allergies(request.allergies() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(request.allergies()))
                .build();
        reservation.setGuest(guest);
        reservation.setGuestName(firstName + " " + lastName);
        reservation.setGuestEmail(email);
        reservation.setGuestPhone(phone);
        reservation.setGuestInfo(guestInfo);
        reservation.setBoatTransferRequested(request.boatTransferRequested());

        // Rechazar IDs duplicados para que cada selección tenga una única línea histórica.
        List<CompleteReservationOnboardingRequest.AdditionalServiceSelection> selections =
                request.additionalServices() == null ? List.of() : request.additionalServices();
        java.util.Set<Long> selectedIds = new java.util.HashSet<>();
        BigDecimal servicesTotal = BigDecimal.ZERO;
        for (CompleteReservationOnboardingRequest.AdditionalServiceSelection selection : selections) {
            if (selection == null) {
                throw new BusinessException("La selección de un servicio adicional no puede estar vacía.");
            }
            if (!selectedIds.add(selection.serviceId())) {
                throw new BusinessException("No se puede seleccionar dos veces el mismo servicio adicional.");
            }
            AdditionalService service = additionalServiceRepository
                    .findByIdAndAccommodation_Id(selection.serviceId(), accommodation.getId())
                    .filter(AdditionalService::isActive)
                    .orElseThrow(() -> new BusinessException(
                            "Uno de los servicios seleccionados no está disponible para este alojamiento."));

            BigDecimal unitPrice = service.getBasePrice();
            boolean perDay = service.getPriceType() == com.hostflow.enums.ServicePriceType.PER_DAY;
            Integer nightsApplied = perDay ? reservation.getNights() : null;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(selection.quantity()));
            if (perDay) {
                lineTotal = lineTotal.multiply(BigDecimal.valueOf(reservation.getNights()));
            }

            ReservationAdditionalService snapshot = ReservationAdditionalService.builder()
                    .additionalService(service)
                    .serviceNameSnapshot(service.getName())
                    .quantity(selection.quantity())
                    .unitPrice(unitPrice)
                    .priceType(service.getPriceType())
                    .nightsApplied(nightsApplied)
                    .lineTotal(lineTotal)
                    .currency(accommodation.getBaseCurrency())
                    .build();
            reservation.addAdditionalService(snapshot);
            servicesTotal = servicesTotal.add(lineTotal);
        }

        // El subtotal de alojamiento y el precio por noche se conservan como snapshots originales.
        reservation.setAdditionalServicesTotal(servicesTotal);
        reservation.setTotalAmount(reservation.getSubtotal().add(servicesTotal));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        // Confirmar el onboarding nunca implica registrar/cobrar el pago.
        reservation.setPaymentStatus(PaymentStatus.PENDING);

        link.markAsUsed();
        reservationRepository.save(reservation);
        linkRepository.save(link);

        List<ReservationOnboardingDetailsResponse.PaymentOption> paymentOptions =
                accommodation.getPaymentMethods() == null ? List.of() : accommodation.getPaymentMethods().stream()
                        .filter(this::isUsablePaymentMethod)
                        .map(this::toPaymentOption)
                        .toList();
        log.info("Onboarding completado para reserva {}. Estado de pago permanece PENDING.",
                reservation.getConfirmationCode());
        return ReservationOnboardingConfirmationResponse.from(reservation, paymentOptions,
                buildCurrencyEquivalents(accommodation, reservation.getPricePerNight(), reservation.getSubtotal(),
                        reservation.getAdditionalServicesTotal(), reservation.getTotalAmount()));
    }

    private void validateOnboardingLink(ReservationLink link) {
        if (!link.isValid()) {
            throw new BusinessException("El enlace de reserva es inválido, ya fue utilizado o ha expirado.");
        }
        Reservation reservation = link.getReservation();
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Este enlace ya no puede utilizarse para confirmar la reserva.");
        }
        if (reservation.getGuestInfo() != null) {
            throw new BusinessException("El onboarding de esta reserva ya fue completado.");
        }
    }

    private Guest resolveGuest(String firstName, String lastName, String email, String phone, String documentNumber) {
        // El correo es un dato de contacto, no una clave única del huésped.
        // Solo reutilizamos el registro cuando coincide el documento de identidad.
        Guest guest = guestRepository.findByDocumentNumber(documentNumber).orElse(null);
        if (guest != null) {
            if (guest.getEmail() != null && !guest.getEmail().equalsIgnoreCase(email)) {
                throw new BusinessException("El documento ya está registrado con otro correo electrónico.");
            }
            guest.setFirstName(firstName);
            guest.setLastName(lastName);
            guest.setFullName(firstName + " " + lastName);
            guest.setPhone(phone);
            return guestRepository.save(guest);
        }

        // Un mismo correo puede pertenecer a distintas personas o utilizarse
        // en varias reservas; por eso no buscamos ni bloqueamos por email.
        Guest newGuest = Guest.builder()
                .firstName(firstName).lastName(lastName).fullName(firstName + " " + lastName)
                .email(email).phone(phone).documentNumber(documentNumber).active(true).build();
        return guestRepository.save(newGuest);
    }

    private List<ReservationOnboardingDetailsResponse.CurrencyEquivalent> buildCurrencyEquivalents(
            Accommodation accommodation, BigDecimal pricePerNight, BigDecimal subtotal,
            BigDecimal additionalServicesTotal, BigDecimal totalAmount) {
        if (!hasActivePaypalPaymentMethod(accommodation)) return List.of();

        List<ReservationOnboardingDetailsResponse.CurrencyEquivalent> equivalents = new java.util.ArrayList<>();
        for (Currency target : List.of(Currency.USD, Currency.EUR)) {
            if (target == accommodation.getBaseCurrency()) continue;
            BigDecimal convertedTotal = currencyConversionService.convertIfAvailable(
                    accommodation.getId(), totalAmount, target);
            if (convertedTotal == null) continue;

            equivalents.add(new ReservationOnboardingDetailsResponse.CurrencyEquivalent(
                    target,
                    currencyConversionService.convertIfAvailable(accommodation.getId(), pricePerNight, target),
                    currencyConversionService.convertIfAvailable(accommodation.getId(), subtotal, target),
                    currencyConversionService.convertIfAvailable(accommodation.getId(), additionalServicesTotal, target),
                    convertedTotal));
        }
        return List.copyOf(equivalents);
    }

    private boolean hasActivePaypalPaymentMethod(Accommodation accommodation) {
        if (accommodation.getPaymentMethods() == null) return false;
        return accommodation.getPaymentMethods().stream()
                .filter(this::isUsablePaymentMethod)
                .anyMatch(method -> containsPaypal(method.getDisplayName()) || containsPaypal(method.getProviderName()));
    }

    private boolean containsPaypal(String value) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains("paypal");
    }

    private ReservationOnboardingDetailsResponse.PaymentOption toPaymentOption(AccommodationPaymentMethod method) {
        return new ReservationOnboardingDetailsResponse.PaymentOption(
                method.getId(), method.getPaymentType(), method.getDisplayName(), method.getCurrency(),
                method.getInstructions(), method.getBankName(), method.getAccountHolder(),
                method.getAccountNumber(), method.getCbuCvu(), method.getAlias(), method.getProviderName(),
                method.getUserIdentifier(), method.getWalletPhone(), method.getWalletEmail());
    }

    // =========================================================================
    // 4. CANCELACIÓN DE RESERVA
    // =========================================================================

    /**
     * Cancela una reserva existente.
     *
     * @param ownerId       ID del propietario autenticado
     * @param reservationId ID de la reserva
     * @param reason        motivo de la cancelación
     * @return la reserva cancelada
     */
    @Transactional
    public ReservationResponse cancelReservation(Long ownerId, Long reservationId, String reason) {
        log.info("❌ Cancelando reserva ID: {} para propietario ID: {}", reservationId, ownerId);

        Reservation reservation = findReservationOwnedBy(ownerId, reservationId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("La reserva ya está cancelada.");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException("No se puede cancelar una reserva que ya ha sido completada.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        linkRepository.findAllByReservation_Id(reservationId).stream()
                .filter(link -> link.getInvalidatedAt() == null && !Boolean.TRUE.equals(link.getUsed()))
                .forEach(link -> { link.invalidate(); linkRepository.save(link); });
        reservation.setCancellationReason(reason);
        reservation.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("✅ Reserva cancelada: {}", reservation.getConfirmationCode());

        // Datos necesarios fuera de la sesión Hibernate para el afterCommit.
        final String guestEmail = reservation.getGuest() != null
                ? reservation.getGuest().getEmail()
                : reservation.getGuestEmail();
        final String guestName = reservation.getGuest() != null
                ? reservation.getGuest().getFullName()
                : reservation.getGuestName();
        final String confirmationCode = reservation.getConfirmationCode();
        final String accommodationName = reservation.getAccommodation().getName();
        final String cancellationReason = reason;

        // El correo de cancelación no debe revertir la cancelación ya persistida.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendReservationCancelledEmail(
                            guestEmail, guestName, confirmationCode, accommodationName, cancellationReason);
                    log.info("📧 Correo de cancelación aceptado por el proveedor para: {}", guestEmail);
                } catch (RuntimeException ex) {
                    log.error("Reserva {} cancelada, pero no se pudo notificar al huésped {}: {}",
                            confirmationCode, guestEmail, ex.getMessage(), ex);
                }
            }
        });

        return ReservationResponse.from(reservation, null);
    }

    // =========================================================================
    // 5. REGISTRAR PAGO RECIBIDO Y CONFIRMAR POR CORREO
    // =========================================================================

    /**
     * Registra manualmente el pago después de que el propietario verifica el
     * comprobante recibido por WhatsApp. Los correos solo se envían cuando la
     * transición PENDING -> PAID queda confirmada en la base de datos.
     */
    @Transactional
    public ReservationResponse markPaymentAsPaid(Long ownerId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reserva con ID: " + reservationId));

        Long reservationOwnerId = reservation.getAccommodation() != null
                && reservation.getAccommodation().getOwner() != null
                ? reservation.getAccommodation().getOwner().getId()
                : null;
        if (!Objects.equals(ownerId, reservationOwnerId)) {
            throw new ResourceNotFoundException("No se encontró la reserva con ID: " + reservationId);
        }

        // Una repetición de la solicitud es idempotente: no duplica los correos.
        if (reservation.getPaymentStatus() == PaymentStatus.PAID) {
            return ReservationResponse.from(reservation, null);
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                    "Solo se puede registrar el pago de una reserva CONFIRMED. Estado actual: "
                            + reservation.getStatus());
        }
        if (reservation.getGuest() == null || reservation.getGuestInfo() == null) {
            throw new BusinessException(
                    "El huésped debe completar sus datos antes de registrar el pago.");
        }

        Owner owner = reservation.getAccommodation().getOwner();
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setPaidAt(LocalDateTime.now());
        reservation.setPaymentMarkedBy(owner);
        reservationRepository.save(reservation);

        // Inicializa las colecciones lazy que las plantillas y la condición PayPal necesitan
        // antes de ejecutar los envíos en afterCommit.
        reservation.getGuestInfo().getAllergies().size();
        reservation.getAccommodation().getPaymentMethods().size();
        String guestName = reservation.getGuest().getFullName();
        String ownerName = owner.getFullName();
        Accommodation accommodation = reservation.getAccommodation();

        // Los emails nunca deben enviarse si la transacción termina en rollback.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendReservationConfirmedGuestEmail(
                            reservation, accommodation, guestName, "#");
                } catch (RuntimeException ex) {
                    // El pago ya quedó registrado; un fallo de correo no debe revertirlo.
                    log.error("No se pudo enviar la confirmación al huésped de la reserva {}: {}",
                            reservation.getConfirmationCode(), ex.getMessage(), ex);
                }

                try {
                    emailService.sendReservationConfirmedOwnerEmail(
                            reservation, accommodation, ownerName, "#");
                } catch (RuntimeException ex) {
                    log.error("No se pudo enviar la notificación al propietario de la reserva {}: {}",
                            reservation.getConfirmationCode(), ex.getMessage(), ex);
                }
            }
        });

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
     * @param ownerId       ID del propietario autenticado
     * @param reservationId ID de la reserva a completar
     * @return la reserva actualizada
     * @throws BusinessException si la reserva no está en estado CONFIRMED o el check-out no ha pasado
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional
    public ReservationResponse completeReservation(Long ownerId, Long reservationId) {
        log.info("✅ Completando reserva ID: {} para propietario ID: {}", reservationId, ownerId);

        Reservation reservation = findReservationOwnedBy(ownerId, reservationId);

        // Solo se pueden completar reservas CONFIRMED
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                    "Solo se pueden completar reservas en estado CONFIRMED. " +
                            "Estado actual: " + reservation.getStatus().getDisplayName());
        }

        // Validar que la fecha de check-out ya haya pasado
        // Sin una hora de salida configurada, la reserva se completa desde el día siguiente al check-out.
        if (!reservation.getCheckOut().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "La reserva solo puede completarse a partir del día siguiente al check-out. " +
                            "Check-out: " + reservation.getCheckOut());
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
    public ReservationResponse getReservationById(Long ownerId, Long reservationId) {
        Reservation reservation = findReservationOwnedBy(ownerId, reservationId);

        ReservationLinkResponse linkResponse = linkRepository.findFirstByReservation_IdOrderByCreatedAtDescIdDesc(reservationId)
                .map(ReservationLinkResponse::from)
                .orElse(null);

        return ReservationResponse.from(reservation, linkResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationByConfirmationCode(String confirmationCode) {
        Reservation reservation = reservationRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la reserva con código: " + confirmationCode));

        ReservationLinkResponse linkResponse = linkRepository.findFirstByReservation_IdOrderByCreatedAtDescIdDesc(reservation.getId())
                .map(ReservationLinkResponse::from)
                .orElse(null);

        return ReservationResponse.from(reservation, linkResponse);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservationsByAccommodation(Long accommodationId) {
        return reservationRepository.findAllByAccommodation_IdOrderByCheckInDesc(accommodationId)
                .stream()
                .map(r -> {
                    ReservationLinkResponse link = linkRepository.findFirstByReservation_IdOrderByCreatedAtDescIdDesc(r.getId())
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
                    ReservationLinkResponse link = linkRepository.findFirstByReservation_IdOrderByCreatedAtDescIdDesc(r.getId())
                            .map(ReservationLinkResponse::from)
                            .orElse(null);
                    return ReservationResponse.from(r, link);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isAccommodationAvailable(Long accommodationId, LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);
        return !reservationRepository.existsOverlappingReservation(accommodationId, checkIn, checkOut);
    }

    // =========================================================================
    // 7. MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Recupera una reserva únicamente si pertenece a un alojamiento del propietario
     * autenticado. Se devuelve el mismo 404 para IDs inexistentes y ajenos para no
     * revelar la existencia de reservas de otros propietarios.
     */
    private Reservation findReservationOwnedBy(Long ownerId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la reserva con ID: " + reservationId));

        Long reservationOwnerId = reservation.getAccommodation() != null
                && reservation.getAccommodation().getOwner() != null
                ? reservation.getAccommodation().getOwner().getId()
                : null;

        if (!Objects.equals(ownerId, reservationOwnerId)) {
            throw new ResourceNotFoundException(
                    "No se encontró la reserva con ID: " + reservationId);
        }
        return reservation;
    }

    /**
     * Valida que las fechas de check-in y check-out sean lógicas y futuras.
     */
    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException("Las fechas de check-in y check-out son obligatorias.");
        }
        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today)) {
            throw new BusinessException("La fecha de check-in no puede ser anterior al día de hoy.");
        }
        if (checkOut.isBefore(today)) {
            throw new BusinessException("La fecha de check-out no puede ser anterior al día de hoy.");
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
    private void validateAvailability(Long accommodationId, LocalDate checkIn, LocalDate checkOut) {
        validateAvailability(accommodationId, checkIn, checkOut, null);
    }

    /**
     * 🛡️ VALIDACIÓN CRÍTICA: Verifica que no haya reservas overlapping,
     * excluyendo una reserva específica (útil al re-confirmar).
     */
    private void validateAvailability(Long accommodationId, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        List<Reservation> overlapping = excludeReservationId == null
                ? reservationRepository.findOverlappingReservations(accommodationId, checkIn, checkOut)
                : reservationRepository.findOverlappingReservationsExcludingId(
                        accommodationId, checkIn, checkOut, excludeReservationId);

        if (!overlapping.isEmpty()) {
            log.warn("⚠️ Conflicto de disponibilidad detectado para alojamiento ID: {}", accommodationId);
            throw new BusinessException("El alojamiento no está disponible en las fechas solicitadas. Ya existe una reserva activa en ese período.");
        }
    }

    /**
     * Calcula el número de noches entre check-in y check-out.
     */
    private int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut));
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
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
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
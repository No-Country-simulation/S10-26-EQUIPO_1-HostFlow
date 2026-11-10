package com.hostflow.service;

import com.hostflow.enums.PaymentStatus;
import com.hostflow.enums.ReservationStatus;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.model.Reservation;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.AdditionalServiceRepository;
import com.hostflow.repository.GuestRepository;
import com.hostflow.repository.ReservationLinkRepository;
import com.hostflow.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Reglas de producto Fase D: cancelar PAID, completar solo con PAID y tras check-out,
 * idempotencia de mark-paid.
 */
class ReservationLifecyclePolicyTest {

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ReservationLinkRepository linkRepository = mock(ReservationLinkRepository.class);
    private final AccommodationRepository accommodationRepository = mock(AccommodationRepository.class);
    private final AdditionalServiceRepository additionalServiceRepository = mock(AdditionalServiceRepository.class);
    private final GuestRepository guestRepository = mock(GuestRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final CurrencyConversionService currencyConversionService = mock(CurrencyConversionService.class);

    private ReservationService service;
    private Owner owner;
    private Accommodation accommodation;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationRepository,
                linkRepository,
                accommodationRepository,
                additionalServiceRepository,
                guestRepository,
                emailService,
                currencyConversionService
        );
        ReflectionTestUtils.setField(service, "frontendConfirmUrl", "http://localhost:3000/reservations/confirm");

        owner = new Owner();
        owner.setId(1L);
        owner.setFullName("Owner Test");

        accommodation = new Accommodation();
        accommodation.setId(10L);
        accommodation.setName("Casa Test");
        accommodation.setOwner(owner);

        reservation = Reservation.builder()
                .id(100L)
                .confirmationCode("HF-TEST01")
                .accommodation(accommodation)
                .guestName("Ana Pérez")
                .guestEmail("ana@example.com")
                .checkIn(LocalDate.now().minusDays(5))
                .checkOut(LocalDate.now().minusDays(2))
                .nights(3)
                .pricePerNight(new BigDecimal("10000"))
                .subtotal(new BigDecimal("30000"))
                .totalAmount(new BigDecimal("30000"))
                .status(ReservationStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .guestCount(2)
                .build();

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(linkRepository.findAllByReservation_Id(100L)).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cancelAllowsPaidReservationAndDoesNotRefund() {
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        var response = service.cancelReservation(1L, 100L, "Huésped no se presentó");

        assertEquals(ReservationStatus.CANCELLED, response.status());
        verify(reservationRepository).save(any(Reservation.class));
        // Correo en afterCommit: no se invoca fuera de transacción Spring real
    }

    @Test
    void cancelRejectsCompletedReservation() {
        reservation.setStatus(ReservationStatus.COMPLETED);

        assertThrows(BusinessException.class,
                () -> service.cancelReservation(1L, 100L, "tarde"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void completeRequiresPaidStatus() {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PENDING);
        reservation.setCheckOut(LocalDate.now().minusDays(1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.completeReservation(1L, 100L));
        assertTrue(ex.getMessage().toLowerCase().contains("pago")
                || ex.getMessage().contains("PAID"));
    }

    @Test
    void completeRequiresCheckoutDayPassed() {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setCheckOut(LocalDate.now()); // hoy: aún no

        assertThrows(BusinessException.class,
                () -> service.completeReservation(1L, 100L));
    }

    @Test
    void completeSucceedsWhenConfirmedPaidAndCheckoutPassed() {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setCheckOut(LocalDate.now().minusDays(1));

        var response = service.completeReservation(1L, 100L);

        assertEquals(ReservationStatus.COMPLETED, response.status());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void markPaidIsIdempotentWhenAlreadyPaid() {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);

        when(reservationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(reservation));

        var first = service.markPaymentAsPaid(1L, 100L);
        var second = service.markPaymentAsPaid(1L, 100L);

        assertEquals(PaymentStatus.PAID, first.paymentStatus());
        assertEquals(PaymentStatus.PAID, second.paymentStatus());
        // No reenvía flujo de transición: save solo si hubiera cambiado; en idempotente no debe re-guardar transición
        verify(emailService, never()).sendReservationConfirmedGuestEmail(any(), any(), any(), any());
    }
}

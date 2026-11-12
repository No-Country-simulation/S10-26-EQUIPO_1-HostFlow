package com.hostflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostflow.enums.*;
import com.hostflow.model.*;
import com.hostflow.repository.*;
import com.hostflow.service.EmailService;
import com.hostflow.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicReservationOnboardingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired OwnerRepository owners;
    @Autowired AccommodationRepository accommodations;
    @Autowired ReservationRepository reservations;
    @Autowired ReservationLinkRepository links;
    @Autowired AdditionalServiceRepository services;
    @Autowired GuestRepository guests;
    @Autowired JdbcTemplate jdbcTemplate;
    @SpyBean EmailService emailService;
    @Autowired ReservationService reservationService;
    @Autowired PlatformTransactionManager transactionManager;

    private Reservation reservation;
    private ReservationLink link;
    private AdditionalService breakfast;
    private Long ownerId;


    @BeforeEach
    void setUp() {
        links.deleteAll(); reservations.deleteAll(); services.deleteAll(); guests.deleteAll();
        accommodations.deleteAll(); owners.deleteAll();

        Owner owner = new Owner();
        owner.setFullName("Propietario Test"); owner.setAccommodationName("HostFlow Test");
        owner.setEmail("owner-" + UUID.randomUUID() + "@example.test"); owner.setPassword("test-password");
        owner.setTermsAccepted(true); owner.setEnabled(true); owner.setEmailVerified(true);
        owner = owners.saveAndFlush(owner);
        ownerId = owner.getId();

        Accommodation accommodation = new Accommodation();
        accommodation.setOwner(owner); accommodation.setName("Cabañas del Lago");
        accommodation.setBaseCurrency(Currency.ARS); accommodation.setBasePrice(new BigDecimal("10000.00"));
        accommodation.setPrimaryColor("#0fa3a3"); accommodation.setAccentColor("#d4f53c");
        accommodation.setTertiaryColor("#f4b8c4"); accommodation.setBackgroundPattern("none");
        accommodation = accommodations.saveAndFlush(accommodation);

        reservation = Reservation.builder().confirmationCode("HF-" + UUID.randomUUID().toString().substring(0, 8))
                .accommodation(accommodation).guestName("Huésped pendiente").guestEmail("preliminar@example.test")
                .guestPhone("+541112345678").checkIn(LocalDate.now().plusDays(10))
                .checkOut(LocalDate.now().plusDays(13))
                .nights(3).guestCount(2).currency(Currency.ARS).pricePerNight(new BigDecimal("10000.00"))
                .subtotal(new BigDecimal("30000.00")).additionalServicesTotal(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("30000.00")).status(ReservationStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING).boatTransferRequested(false).build();
        reservation = reservations.saveAndFlush(reservation);
        link = links.saveAndFlush(ReservationLink.builder().reservation(reservation).token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24)).used(false).confirmationUrl("https://test.example/confirm").build());
        breakfast = services.saveAndFlush(AdditionalService.builder().accommodation(accommodation).name("Desayuno")
                .description("Desayuno por persona y día").basePrice(new BigDecimal("1500.00"))
                .priceType(ServicePriceType.PER_DAY).active(true).build());
    }

    @Test
    void getDetailsReturnsPublicDataAndDoesNotConsumeToken() throws Exception {
        mvc.perform(get("/public/reservations/confirm").param("token", link.getToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.confirmationCode").value(reservation.getConfirmationCode()))
                .andExpect(jsonPath("$.accommodationName").value("Cabañas del Lago"))
                .andExpect(jsonPath("$.nights").value(3)).andExpect(jsonPath("$.additionalServices[0].name").value("Desayuno"));
        mvc.perform(get("/public/reservations/confirm").param("token", link.getToken())).andExpect(status().isOk());
        assertThat(links.findById(link.getId()).orElseThrow().getUsed()).isFalse();
    }

    @Test
    void completeOnboardingSnapshotsServicePriceAndKeepsPaymentPending() throws Exception {
        String body = requestJson(breakfast.getId(), 2);
        String raw = mvc.perform(post("/public/reservations/confirm").param("token", link.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                .andExpect(jsonPath("$.reservationStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.additionalServicesTotal").value(9000.00))
                .andExpect(jsonPath("$.totalAmount").value(39000.00))
                .andExpect(jsonPath("$.selectedAdditionalServices[0].name").value("Desayuno"))
                .andReturn().getResponse().getContentAsString();
        JsonNode response = mapper.readTree(raw);
        assertThat(response.at("/selectedAdditionalServices/0/unitPrice").decimalValue()).isEqualByComparingTo("1500.00");
        Reservation saved = reservations.findById(reservation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getPaidAt()).isNull();
        assertThat(links.findById(link.getId()).orElseThrow().getUsed()).isTrue();
    }

    @Test
    void tokenCannotBeUsedTwice() throws Exception {
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken()).contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(null, 0))).andExpect(status().isOk());
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken()).contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(null, 0))).andExpect(status().is4xxClientError());
        assertThat(reservations.findById(reservation.getId()).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(guests.count()).isEqualTo(1);
    }


    @Test
    void sameEmailCanBeUsedForDifferentReservations() throws Exception {
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(null, 0)))
                .andExpect(status().isOk());

        Reservation secondReservation = Reservation.builder()
                .confirmationCode("HF-" + UUID.randomUUID().toString().substring(0, 8))
                .accommodation(reservation.getAccommodation())
                .guestName("Huésped pendiente")
                .guestEmail("preliminar@example.test")
                .guestPhone("+541112345678")
                .checkIn(LocalDate.now().plusDays(20))
                .checkOut(LocalDate.now().plusDays(22))
                .nights(2).guestCount(1).currency(Currency.ARS)
                .pricePerNight(new BigDecimal("10000.00"))
                .subtotal(new BigDecimal("20000.00")).additionalServicesTotal(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("20000.00")).status(ReservationStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING).boatTransferRequested(false).build();
        secondReservation = reservations.saveAndFlush(secondReservation);
        ReservationLink secondLink = links.saveAndFlush(ReservationLink.builder()
                .reservation(secondReservation).token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24)).used(false)
                .confirmationUrl("https://test.example/confirm").build());

        String sameEmailDifferentDocument = requestJson(null, 0)
                .replace("30123456", "30123457");
        mvc.perform(post("/public/reservations/confirm").param("token", secondLink.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(sameEmailDifferentDocument))
                .andExpect(status().isOk());

        Reservation firstSaved = reservations.findById(reservation.getId()).orElseThrow();
        Reservation secondSaved = reservations.findById(secondReservation.getId()).orElseThrow();
        assertThat(firstSaved.getGuestEmail()).isEqualTo("preliminar@example.test");
        assertThat(secondSaved.getGuestEmail()).isEqualTo("preliminar@example.test");
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM reservation_guest_info WHERE reservation_id = ?",
                String.class, firstSaved.getId())).isEqualTo("lucia.gomez@example.test");
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM reservation_guest_info WHERE reservation_id = ?",
                String.class, secondSaved.getId())).isEqualTo("lucia.gomez@example.test");
        assertThat(guests.count()).isEqualTo(2);
    }

    @Test
    void existingGuestProfileIsUpdatedAndPreviousEmailIsNotifiedAfterOnboarding() throws Exception {
        reservation.setGuestName("Lucía - nombre indicado por owner");
        reservation.setGuestEmail("owner-contact@example.test");
        reservation.setGuestPhone("+549110000000");
        reservation = reservations.saveAndFlush(reservation);

        Guest existing = guests.saveAndFlush(Guest.builder()
                .firstName("Lucía").lastName("Gómez").fullName("Lucía Gómez")
                .email("lucia.old@example.test").phone("+541100000000")
                .documentNumber("30123456").active(true).build());
        doNothing().when(emailService).sendGuestEmailChangedNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());

        String body = requestJson(null, 0);
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        Guest updated = guests.findById(existing.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Lucía");
        assertThat(updated.getLastName()).isEqualTo("Gómez");
        assertThat(updated.getEmail()).isEqualTo("lucia.gomez@example.test");
        assertThat(updated.getPhone()).isEqualTo("+541112345678");

        Reservation savedReservation = reservations.findById(reservation.getId()).orElseThrow();
        assertThat(savedReservation.getGuest()).isNotNull();
        assertThat(savedReservation.getGuest().getId()).isEqualTo(existing.getId());
        // Los datos originales capturados por el owner no se sobrescriben.
        assertThat(savedReservation.getGuestEmail()).isEqualTo("owner-contact@example.test");
        assertThat(savedReservation.getGuestName()).isEqualTo("Lucía - nombre indicado por owner");
        assertThat(savedReservation.getGuestPhone()).isEqualTo("+549110000000");
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM reservation_guest_info WHERE reservation_id = ?",
                String.class, savedReservation.getId())).isEqualTo("lucia.gomez@example.test");
        verify(emailService, times(1)).sendGuestEmailChangedNotification(
                "lucia.old@example.test", "Lucía Gómez", "l***@example.test");
    }

    @Test
    void sameDocumentAndSameEmailReuseExistingGuestProfile() throws Exception {
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(null, 0)))
                .andExpect(status().isOk());
        Long originalGuestId = reservations.findById(reservation.getId()).orElseThrow().getGuest().getId();

        Reservation second = createSecondReservation();
        ReservationLink secondLink = createLink(second);
        mvc.perform(post("/public/reservations/confirm").param("token", secondLink.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(null, 0)))
                .andExpect(status().isOk());

        Reservation savedSecond = reservations.findById(second.getId()).orElseThrow();
        assertThat(savedSecond.getGuest().getId()).isEqualTo(originalGuestId);
        assertThat(guests.count()).isEqualTo(1);
        verify(emailService, never()).sendGuestEmailChangedNotification(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void failedPreviousEmailNotificationDoesNotRollbackOnboarding() throws Exception {
        Guest existing = guests.saveAndFlush(Guest.builder().firstName("Lucía").lastName("Gómez")
                .fullName("Lucía Gómez").email("old@example.test").phone("+541100000000")
                .documentNumber("30123456").active(true).build());
        doThrow(new IllegalStateException("SMTP unavailable")).when(emailService)
                .sendGuestEmailChangedNotification("old@example.test", "Lucía Gómez", "l***@example.test");

        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(null, 0)))
                .andExpect(status().isOk());

        Guest updated = guests.findById(existing.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("lucia.gomez@example.test");
        assertThat(reservations.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(links.findById(link.getId()).orElseThrow().getUsed()).isTrue();
    }

    @Test
    void sameEmailDoesNotTriggerSecurityNotification() throws Exception {
        Guest existing = guests.saveAndFlush(Guest.builder().firstName("Lucía").lastName("Gómez")
                .fullName("Lucía Gómez").email("lucia.gomez@example.test").phone("+541112345678")
                .documentNumber("30123456").active(true).build());

        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(null, 0)))
                .andExpect(status().isOk());

        assertThat(guests.findById(existing.getId()).orElseThrow().getEmail())
                .isEqualTo("lucia.gomez@example.test");
        verify(emailService, never()).sendGuestEmailChangedNotification(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }


    @Test
    void historicalReservationGuestInfoDoesNotChangeWhenGuestProfileIsUpdatedLater() throws Exception {
        doNothing().when(emailService).sendGuestEmailChangedNotification(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(null, 0)))
                .andExpect(status().isOk());
        Long guestId = reservations.findById(reservation.getId()).orElseThrow().getGuest().getId();

        Reservation second = createSecondReservation();
        ReservationLink secondLink = createLink(second);
        String updatedProfile = requestJson(null, 0)
                .replace("Lucía", "Lucía María")
                .replace("lucia.gomez@example.test", "lucia.nueva@example.test")
                .replace("+541112345678", "+5491198765432");
        mvc.perform(post("/public/reservations/confirm").param("token", secondLink.getToken())
                        .contentType(MediaType.APPLICATION_JSON).content(updatedProfile))
                .andExpect(status().isOk());

        assertThat(reservations.findById(second.getId()).orElseThrow().getGuest().getId()).isEqualTo(guestId);
        assertThat(guests.findById(guestId).orElseThrow().getEmail()).isEqualTo("lucia.nueva@example.test");
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM reservation_guest_info WHERE reservation_id = ?",
                String.class, reservation.getId())).isEqualTo("lucia.gomez@example.test");
        assertThat(jdbcTemplate.queryForObject("SELECT first_name FROM reservation_guest_info WHERE reservation_id = ?",
                String.class, reservation.getId())).isEqualTo("Lucía");
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM reservation_guest_info WHERE reservation_id = ?",
                String.class, second.getId())).isEqualTo("lucia.nueva@example.test");
        assertThat(jdbcTemplate.queryForObject("SELECT first_name FROM reservation_guest_info WHERE reservation_id = ?",
                String.class, second.getId())).isEqualTo("Lucía María");
    }

    @Test
    void cancellationPersistsAndSendsEmailOnlyAfterCommit() {
        doNothing().when(emailService).sendReservationCancelledEmail(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation = reservations.saveAndFlush(reservation);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            reservationService.cancelReservation(
                    ownerId,
                    reservation.getId(),
                    "Cancelación solicitada por el propietario"
            );
            // La notificación debe esperar al commit de la transacción externa.
            verify(emailService, never()).sendReservationCancelledEmail(
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString());
        });

        Reservation cancelled = reservations.findById(reservation.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Cancelación solicitada por el propietario");
        assertThat(links.findById(link.getId()).orElseThrow().getInvalidatedAt()).isNotNull();
        verify(emailService, times(1)).sendReservationCancelledEmail(
                "preliminar@example.test", "Huésped pendiente", reservation.getConfirmationCode(),
                "Cabañas del Lago", "Cancelación solicitada por el propietario");
    }

    @Test
    void cancellationRemainsPersistedWhenCancellationEmailFails() {
        doThrow(new IllegalStateException("SMTP unavailable")).when(emailService)
                .sendReservationCancelledEmail(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        reservationService.cancelReservation(reservation.getAccommodation().getOwner().getId(),
                reservation.getId(), "Prueba de fallo de correo");

        assertThat(reservations.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(links.findById(link.getId()).orElseThrow().getInvalidatedAt()).isNotNull();
        verify(emailService, times(1)).sendReservationCancelledEmail(
                "preliminar@example.test", "Huésped pendiente", reservation.getConfirmationCode(),
                "Cabañas del Lago", "Prueba de fallo de correo");
    }

    private Reservation createSecondReservation() {
        Reservation second = Reservation.builder()
                .confirmationCode("HF-" + UUID.randomUUID().toString().substring(0, 8))
                .accommodation(reservation.getAccommodation()).guestName("Huésped segundo")
                .guestEmail("second-owner@example.test").guestPhone("+541199999999")
                .checkIn(LocalDate.now().plusDays(20)).checkOut(LocalDate.now().plusDays(22))
                .nights(2).guestCount(1).currency(Currency.ARS).pricePerNight(new BigDecimal("10000.00"))
                .subtotal(new BigDecimal("20000.00")).additionalServicesTotal(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("20000.00")).status(ReservationStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING).boatTransferRequested(false).build();
        return reservations.saveAndFlush(second);
    }

    private ReservationLink createLink(Reservation target) {
        return links.saveAndFlush(ReservationLink.builder().reservation(target)
                .token(UUID.randomUUID().toString()).expiresAt(LocalDateTime.now().plusHours(24))
                .used(false).confirmationUrl("https://test.example/confirm").build());
    }

    @Test
    void serviceFromDifferentAccommodationIsRejectedAndTransactionRollsBack() throws Exception {
        Owner owner2 = new Owner(); owner2.setFullName("Otro Propietario"); owner2.setAccommodationName("Otro");
        owner2.setEmail("other-" + UUID.randomUUID() + "@example.test"); owner2.setPassword("test-password");
        owner2.setTermsAccepted(true); owner2.setEnabled(true); owner2.setEmailVerified(true); owner2 = owners.saveAndFlush(owner2);
        Accommodation other = new Accommodation(); other.setOwner(owner2); other.setName("Otro alojamiento");
        other.setBaseCurrency(Currency.ARS); other.setPrimaryColor("#0fa3a3"); other.setAccentColor("#d4f53c");
        other.setTertiaryColor("#f4b8c4"); other.setBackgroundPattern("none"); other = accommodations.saveAndFlush(other);
        AdditionalService foreign = services.saveAndFlush(AdditionalService.builder().accommodation(other).name("Ajeno")
                .basePrice(new BigDecimal("500")).priceType(ServicePriceType.TOTAL).active(true).build());
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken()).contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(foreign.getId(), 1))).andExpect(status().is4xxClientError());
        Reservation saved = reservations.findById(reservation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(links.findById(link.getId()).orElseThrow().getUsed()).isFalse();
        assertThat(guests.count()).isZero();
    }

    @Test
    void expiredTokenIsRejectedByGet() throws Exception {
        link.setExpiresAt(LocalDateTime.now().minusMinutes(1)); links.saveAndFlush(link);
        mvc.perform(get("/public/reservations/confirm").param("token", link.getToken())).andExpect(status().is4xxClientError());
    }

    @Test
    void invalidBodyIsRejectedWithoutConsumingToken() throws Exception {
        String invalid = "{\"firstName\":\"A\",\"lastName\":\"B\",\"documentNumber\":\"123\",\"email\":\"bad\",\"phone\":\"123\",\"allergies\":[],\"additionalServices\":[],\"boatTransferRequested\":false}";
        mvc.perform(post("/public/reservations/confirm").param("token", link.getToken()).contentType(MediaType.APPLICATION_JSON)
                .content(invalid)).andExpect(status().isBadRequest());
        assertThat(links.findById(link.getId()).orElseThrow().getUsed()).isFalse();
    }

    private String requestJson(Long serviceId, int quantity) {
        String selections = serviceId == null ? "[]" : "[{\"serviceId\":" + serviceId + ",\"quantity\":" + quantity + "}]";
        return "{\"firstName\":\"Lucía\",\"lastName\":\"Gómez\",\"documentNumber\":\"30123456\","
                + "\"email\":\"lucia.gomez@example.test\",\"phone\":\"+541112345678\",\"allergies\":[\"Maní\"],"
                + "\"preferences\":\"Cama adicional\",\"additionalServices\":" + selections + ",\"boatTransferRequested\":true}";
    }
}

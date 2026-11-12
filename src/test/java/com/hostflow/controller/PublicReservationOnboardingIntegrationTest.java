package com.hostflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostflow.enums.*;
import com.hostflow.model.*;
import com.hostflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private Reservation reservation;
    private ReservationLink link;
    private AdditionalService breakfast;

    @BeforeEach
    void setUp() {
        links.deleteAll(); reservations.deleteAll(); services.deleteAll(); guests.deleteAll();
        accommodations.deleteAll(); owners.deleteAll();

        Owner owner = new Owner();
        owner.setFullName("Propietario Test"); owner.setAccommodationName("HostFlow Test");
        owner.setEmail("owner-" + UUID.randomUUID() + "@example.test"); owner.setPassword("test-password");
        owner.setTermsAccepted(true); owner.setEnabled(true); owner.setEmailVerified(true);
        owner = owners.saveAndFlush(owner);

        Accommodation accommodation = new Accommodation();
        accommodation.setOwner(owner); accommodation.setName("Cabañas del Lago");
        accommodation.setBaseCurrency(Currency.ARS); accommodation.setBasePrice(new BigDecimal("10000.00"));
        accommodation.setPrimaryColor("#0fa3a3"); accommodation.setAccentColor("#d4f53c");
        accommodation.setTertiaryColor("#f4b8c4"); accommodation.setBackgroundPattern("none");
        accommodation = accommodations.saveAndFlush(accommodation);

        reservation = Reservation.builder().confirmationCode("HF-" + UUID.randomUUID().toString().substring(0, 8))
                .accommodation(accommodation).guestName("Huésped pendiente").guestEmail("preliminar@example.test")
                .guestPhone("+541112345678").checkIn(LocalDateTime.now().plusDays(10).withHour(15).withMinute(0).withSecond(0).withNano(0))
                .checkOut(LocalDateTime.now().plusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0))
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

package com.hostflow.controller;

import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.ReservationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Dashboard Admin", description = "Métricas globales de la plataforma HostFlow")
public class AdminDashboardController {

    private final AccommodationRepository accommodationRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping("/metrics")
    @Operation(summary = "Obtener métricas globales del administrador")
    public ResponseEntity<Map<String, Object>> getAdminMetrics() {
        LocalDateTime startOfMonth = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);

        long totalAccommodations = accommodationRepository.countTotalAccommodations();
        long accommodationsLastMonth = accommodationRepository.countAccommodationsLastMonth(startOfMonth);
        long inactiveAccommodations = accommodationRepository.countInactiveAccommodations();
        long totalCompletedReservations = reservationRepository.countTotalCompletedReservations();

        Map<String, Object> metrics = Map.of(
                "totalAccommodations", totalAccommodations,
                "accommodationsLastMonth", accommodationsLastMonth,
                "inactiveAccommodations", inactiveAccommodations,
                "totalCompletedReservations", totalCompletedReservations
        );

        return ResponseEntity.ok(metrics);
    }
}
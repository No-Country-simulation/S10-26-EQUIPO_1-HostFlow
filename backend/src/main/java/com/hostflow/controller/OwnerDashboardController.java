package com.hostflow.controller;

import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.ReservationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@RestController
@RequestMapping("/owners/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Dashboard Owner", description = "Métricas y estadísticas para el propietario")
public class OwnerDashboardController {

    private final ReservationRepository reservationRepository;
    private final OwnerRepository ownerRepository;

    @GetMapping("/metrics")
    @Operation(summary = "Obtener métricas del dashboard del propietario")
    public ResponseEntity<Map<String, Object>> getOwnerMetrics(Authentication auth) {
        Owner owner = ownerRepository.findByEmail(auth.getName()).orElseThrow();
        Long ownerId = owner.getId();

        // Calcular inicio del mes actual
        LocalDateTime startOfMonth = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);

        long activeReservations = reservationRepository.countActiveReservationsByOwnerId(ownerId);
        long completedReservations = reservationRepository.countCompletedReservationsByOwnerId(ownerId);
        BigDecimal incomeLastMonth = reservationRepository.sumIncomeLastMonthByOwnerId(ownerId, startOfMonth);
        BigDecimal accumulatedIncome = reservationRepository.sumAccumulatedIncomeByOwnerId(ownerId);

        Map<String, Object> metrics = Map.of(
                "activeReservations", activeReservations,
                "completedReservations", completedReservations,
                "incomeLastMonth", incomeLastMonth,
                "accumulatedIncome", accumulatedIncome
        );

        return ResponseEntity.ok(metrics);
    }
}
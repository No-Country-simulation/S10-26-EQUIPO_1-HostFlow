package com.hostflow.service;

import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para gestionar las métricas del dashboard del administrador.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final AccommodationRepository accommodationRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Obtiene las métricas globales de la plataforma para el administrador.
     *
     * @return mapa con las métricas del sistema
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminMetrics() {
        log.info("📊 Obteniendo métricas globales del administrador");

        LocalDateTime startOfMonth = LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        long totalAccommodations = accommodationRepository.countTotalAccommodations();
        long accommodationsLastMonth = accommodationRepository.countAccommodationsLastMonth(startOfMonth);
        long inactiveAccommodations = accommodationRepository.countInactiveAccommodations();
        long totalCompletedReservations = reservationRepository.countTotalCompletedReservations();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalAccommodations", totalAccommodations);
        metrics.put("accommodationsLastMonth", accommodationsLastMonth);
        metrics.put("inactiveAccommodations", inactiveAccommodations);
        metrics.put("totalCompletedReservations", totalCompletedReservations);

        log.info("✅ Métricas del administrador obtenidas: {} alojamientos, {} reservas completadas",
                totalAccommodations, totalCompletedReservations);

        return metrics;
    }
}
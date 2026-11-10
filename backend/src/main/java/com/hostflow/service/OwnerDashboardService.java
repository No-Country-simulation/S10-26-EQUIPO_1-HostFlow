package com.hostflow.service;

import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

/**
 * Servicio encargado de gestionar las métricas y estadísticas del dashboard del propietario.
 *
 * <p>Centraliza la lógica de negocio para consultar reservas e ingresos,
 * garantizando que el controlador se mantenga ligero y enfocado únicamente
 * en el manejo de peticiones HTTP.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerDashboardService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    private final OwnerRepository ownerRepository;
    private final ReservationRepository reservationRepository;

    // =========================================================================
    // LÓGICA DE NEGOCIO
    // =========================================================================

    /**
     * Obtiene las métricas financieras y operativas del alojamiento del propietario.
     *
     * <p>Calcula el inicio del mes actual y consulta la base de datos para obtener:
     * <ul>
     *   <li><b>Reservas activas:</b> Reservas en estado CONFIRMED.</li>
     *   <li><b>Reservas completadas:</b> Historial de reservas en estado COMPLETED.</li>
     *   <li><b>Ingresos del mes:</b> Suma de montos de reservas completadas desde el
     *       primer día del mes actual hasta la fecha.</li>
     *   <li><b>Ingresos acumulados:</b> Suma histórica total de todas las reservas completadas.</li>
     * </ul>
     *
     * @param ownerEmail el correo electrónico del propietario autenticado (extraído del JWT)
     * @return un mapa inmutable con las métricas calculadas
     * @throws ResourceNotFoundException si no se encuentra un propietario con ese email
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOwnerMetrics(String ownerEmail) {
        log.debug("📊 Calculando métricas del dashboard para el propietario: {}", ownerEmail);

        // 1. Obtener y validar la existencia del propietario
        Owner owner = ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario asociado al email: " + ownerEmail));

        Long ownerId = owner.getId();

        // 2. Calcular el inicio del mes actual (día 1, 00:00:00.000) para filtrar ingresos recientes
        LocalDateTime startOfMonth = LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // 3. Ejecutar consultas agregadas optimizadas en la capa de repositorio
        long activeReservations = reservationRepository.countActiveReservationsByOwnerId(ownerId);
        long completedReservations = reservationRepository.countCompletedReservationsByOwnerId(ownerId);

        // ⚠️ NOTA: Los métodos del repositorio ya incluyen COALESCE(..., 0) en sus consultas SQL,
        // por lo que garantizan que nunca retornarán null, sino BigDecimal.ZERO si no hay datos.
        BigDecimal incomeCurrentMonth = reservationRepository.sumIncomeLastMonthByOwnerId(ownerId, startOfMonth);
        BigDecimal accumulatedIncome = reservationRepository.sumAccumulatedIncomeByOwnerId(ownerId);

        // 4. Construir la respuesta de forma inmutable y segura (Java 9+)
        // Se mantiene la clave "incomeLastMonth" por compatibilidad con el contrato de la API/Frontend,
        // aunque la lógica calcula los ingresos desde el inicio del mes actual.
        Map<String, Object> metrics = Map.of(
                "activeReservations", activeReservations,
                "completedReservations", completedReservations,
                "incomeLastMonth", incomeCurrentMonth,
                "accumulatedIncome", accumulatedIncome
        );

        log.info("✅ Métricas obtenidas exitosamente para Owner ID {}: {} reservas activas, ingresos del mes: ${}",
                ownerId, activeReservations, incomeCurrentMonth);

        return metrics;
    }
}
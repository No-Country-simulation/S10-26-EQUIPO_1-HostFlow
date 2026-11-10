package com.hostflow.model;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que cachea las tasas de cambio obtenidas desde APIs externas.
 *
 * <p>Actúa como una "memoria intermedia" para evitar consultar las APIs
 * externas en cada petición. Las tasas se refrescan periódicamente
 * (cada 1 hora por defecto) mediante el {@code ExchangeRateProviderService}.</p>
 *
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Una entrada por par (moneda_origen, moneda_destino, tipo_dólar).</li>
 *   <li>Incluye timestamp de la última actualización para detectar obsolescencia.</li>
 *   <li>Almacena la tasa como "cuántas unidades de destino equivalen a 1 unidad de origen".</li>
 * </ul>
 *
 * <p>Ejemplo: Para ARS→USD (blue), si 1 USD = 1170 ARS, entonces
 * {@code rate = 1/1170 ≈ 0.0008547009} (cuántos USD vale 1 ARS).</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exchange_rates_from_to_type",
                columnNames = {"from_currency", "to_currency", "usd_rate_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRateCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Moneda origen (generalmente ARS). */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false, length = 10)
    private Currency fromCurrency;

    /** Moneda destino (USD, EUR). */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false, length = 10)
    private Currency toCurrency;

    /**
     * Tipo de cotización del dólar (solo relevante para USD).
     * Para EUR u otras monedas, se deja en {@code null}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usd_rate_type", length = 20)
    private UsdRateType usdRateType;

    /**
     * Tasa de conversión: cuántas unidades de {@code toCurrency} equivalen a 1 unidad de {@code fromCurrency}.
     * Ejemplo: 1 ARS = 0.0008547009 USD → rate = 0.0008547009
     */
    @Column(name = "rate", nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    /** Fecha y hora en que se obtuvo esta tasa desde la API externa. */
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}
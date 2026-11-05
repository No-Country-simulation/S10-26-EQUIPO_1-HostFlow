package com.hostflow.service;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.AcceptedCurrency;
import com.hostflow.model.Accommodation;
import com.hostflow.repository.AcceptedCurrencyRepository;
import com.hostflow.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio para gestionar las monedas habilitadas de un alojamiento.
 *
 * <p>Permite al propietario:</p>
 * <ul>
 *   <li>Listar las monedas configuradas de su alojamiento.</li>
 *   <li>Habilitar o deshabilitar monedas extranjeras (USD, EUR).</li>
 *   <li>Elegir entre tasa automática (obtenida de APIs externas) o tasa manual.</li>
 *   <li>Para USD: elegir el tipo de cotización específico (blue, oficial, MEP, etc.).</li>
 * </ul>
 *
 * <p><strong>Reglas de negocio estrictas:</strong></p>
 * <ul>
 *   <li>La moneda base del alojamiento (generalmente ARS) siempre está habilitada
 *       con tasa 1:1 y no puede ser desactivada ni modificada.</li>
 *   <li>Si se configura una tasa manual, esta debe ser estrictamente mayor a cero.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcceptedCurrencyService {

    private final AcceptedCurrencyRepository repository;
    private final AccommodationRepository accommodationRepository;

    /**
     * Lista todas las monedas configuradas para el alojamiento del propietario.
     *
     * @param ownerId el identificador único del propietario autenticado
     * @return lista de configuraciones de moneda ordenadas alfabéticamente
     */
    @Transactional(readOnly = true)
    public List<AcceptedCurrency> list(Long ownerId) {
        Accommodation acc = getAccommodation(ownerId);
        return repository.findAllByAccommodation_IdOrderByCurrencyAsc(acc.getId());
    }

    /**
     * Crea o actualiza la configuración de una moneda para el alojamiento (Upsert).
     *
     * @param ownerId          el identificador único del propietario autenticado
     * @param currency         la moneda a configurar (ej. {@link Currency#USD}, {@link Currency#EUR})
     * @param enabled          si la moneda está habilitada para cobro
     * @param useAutomaticRate si se usa la tasa automática de la API externa
     * @param manualRate       la tasa manual de respaldo (solo si useAutomaticRate es false)
     * @param usdRateType      el tipo de cotización del dólar (solo aplica si currency es USD)
     * @return la configuración de moneda persistida en la base de datos
     * @throws BusinessException si se intenta desactivar la moneda base o la tasa manual es inválida
     */
    @Transactional
    public AcceptedCurrency upsert(Long ownerId, Currency currency, boolean enabled,
                                   boolean useAutomaticRate, BigDecimal manualRate,
                                   UsdRateType usdRateType) {
        Accommodation acc = getAccommodation(ownerId);

        // Regla de negocio: La moneda base (ARS) siempre está habilitada con tasa 1
        if (currency == acc.getBaseCurrency()) {
            enabled = true;
            useAutomaticRate = false;
            manualRate = BigDecimal.ONE;
        }

        // Validación: si se usa tasa manual, debe ser un valor válido (> 0)
        if (!useAutomaticRate && currency != acc.getBaseCurrency()) {
            if (manualRate == null || manualRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Debe indicar una tasa manual válida mayor a 0.");
            }
        }

        // Buscar configuración existente o preparar una nueva
        AcceptedCurrency entity = repository
                .findByAccommodation_IdAndCurrency(acc.getId(), currency)
                .orElse(AcceptedCurrency.builder()
                        .accommodation(acc)
                        .currency(currency)
                        .build());

        // Aplicar los nuevos valores
        entity.setEnabled(enabled);
        entity.setUseAutomaticRate(useAutomaticRate);

        if (useAutomaticRate) {
            entity.setManualRate(null); // Limpiar tasa manual si se usa la automática

            // Para USD, guardar el tipo de cotización elegido por el propietario
            if (currency == Currency.USD) {
                entity.setUsdRateType(usdRateType != null ? usdRateType : UsdRateType.BLUE);
            } else {
                entity.setUsdRateType(null);
            }
        } else {
            entity.setManualRate(manualRate);
            entity.setUsdRateType(null); // No aplica tipo de dólar si la tasa es manual
        }

        AcceptedCurrency saved = repository.save(entity);

        log.info("💱 Moneda {} {} para alojamiento {} (Tasa: {})",
                currency,
                enabled ? "habilitada" : "deshabilitada",
                acc.getId(),
                useAutomaticRate ? "Automática (API)" : "Manual (" + manualRate + ")");

        return saved;
    }

    /**
     * Obtiene el alojamiento asociado al propietario o lanza una excepción si no existe.
     *
     * @param ownerId el identificador único del propietario
     * @return la entidad Accommodation asociada
     * @throws ResourceNotFoundException si el propietario aún no ha registrado su alojamiento
     */
    private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento antes de configurar monedas."));
    }
}
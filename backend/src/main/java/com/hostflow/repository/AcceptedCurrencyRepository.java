package com.hostflow.repository;

import com.hostflow.enums.Currency;
import com.hostflow.model.AcceptedCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la gestión de la entidad {@link AcceptedCurrency}.
 *
 * <p>Extiende {@link JpaRepository} para heredar operaciones CRUD básicas
 * (crear, leer, actualizar, eliminar) y proporciona métodos de consulta
 * personalizados basados en la convención de nombres de Spring Data JPA.</p>
 *
 * <p>Este repositorio es utilizado por los servicios de configuración de
 * alojamiento para gestionar las monedas habilitadas (ARS, USD, EUR) y
 * sus respectivas tasas de conversión (automáticas o manuales).</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see AcceptedCurrency
 */
@Repository
public interface AcceptedCurrencyRepository extends JpaRepository<AcceptedCurrency, Long> {

    /**
     * Busca todas las configuraciones de moneda para un alojamiento específico,
     * ordenadas alfabéticamente por el nombre de la moneda (Enum).
     *
     * <p>Este método es utilizado principalmente para mostrar al propietario
     * el estado actual de todas las monedas configuradas en su panel de control,
     * independientemente de si están habilitadas o deshabilitadas.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @return una lista de configuraciones de moneda, ordenada por {@link Currency}
     */
    List<AcceptedCurrency> findAllByAccommodation_IdOrderByCurrencyAsc(Long accommodationId);

    /**
     * Busca la configuración de una moneda específica para un alojamiento determinado.
     *
     * <p>Se utiliza para verificar si una moneda (ej. USD o EUR) ya está configurada
     * antes de crear un nuevo registro (upsert), o para recuperar su tasa y estado
     * de activación durante el proceso de conversión de precios en tiempo real.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @param currency        la moneda a buscar (ej. {@link Currency#USD}, {@link Currency#EUR})
     * @return un {@link Optional} que contiene la configuración si existe, o vacío si no está configurada
     */
    Optional<AcceptedCurrency> findByAccommodation_IdAndCurrency(Long accommodationId, Currency currency);

    /**
     * Verifica si existe una configuración de moneda específica para un alojamiento.
     *
     * <p>Este método es más eficiente que {@link #findByAccommodation_IdAndCurrency}
     * cuando solo se necesita saber si el registro existe (ej. para validaciones
     * de unicidad antes de intentar una inserción), ya que ejecuta una consulta
     * {@code EXISTS} o {@code COUNT} en la base de datos en lugar de traer
     * toda la entidad a la memoria de la aplicación.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @param currency        la moneda a verificar
     * @return {@code true} si la configuración ya existe, {@code false} en caso contrario
     */
    boolean existsByAccommodation_IdAndCurrency(Long accommodationId, Currency currency);

    /**
     * Busca todas las monedas que están actualmente habilitadas ({@code enabled = true})
     * para un alojamiento específico.
     *
     * <p>Este método es crucial para los controladores públicos (ej. {@code PublicPricingController}),
     * ya que solo debe mostrar precios convertidos en las monedas que el propietario
     * ha decidido activar explícitamente para el cobro a los huéspedes.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @return una lista de configuraciones de moneda que están activas
     */
    List<AcceptedCurrency> findAllByAccommodation_IdAndEnabledTrue(Long accommodationId);
}
-- ==============================================================================
-- HostFlow - Migración V9: Módulo Multimoneda y Servicios Adicionales
-- ==============================================================================
-- Propósito: Crear las tablas y columnas necesarias para soportar:
--   1. Configuración de monedas aceptadas por alojamiento.
--   2. Caché de tasas de cambio obtenidas desde APIs externas.
--   3. Servicios adicionales con tipo de precio (por día o total).
--   4. Imágenes asociadas a los servicios adicionales (Cloudinary).
--
-- ⚠️ VERSIÓN SEGURA: Se eliminaron los DROP TABLE destructivos que
--    existían en versiones anteriores. Todas las sentencias son
--    idempotentes (IF NOT EXISTS / IF EXISTS) para no romper
--    entornos de producción con datos existentes.
--
-- Autor: HostFlow Team
-- Versión: 6.1.0-safe
-- Base de Datos: PostgreSQL
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Tabla: accepted_currencies
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accepted_currencies (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   accommodation_id BIGINT NOT NULL,
                                                   currency VARCHAR(10) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    use_automatic_rate BOOLEAN NOT NULL DEFAULT TRUE,
    manual_rate NUMERIC(20, 10),
    usd_rate_type VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accepted_currencies_accommodation
    FOREIGN KEY (accommodation_id) REFERENCES accommodations(id) ON DELETE CASCADE,
    CONSTRAINT uk_accepted_currencies_accommodation_currency
    UNIQUE (accommodation_id, currency)
    );

CREATE INDEX IF NOT EXISTS idx_accepted_currencies_accommodation
    ON accepted_currencies(accommodation_id);

-- ------------------------------------------------------------------------------
-- 2. Tabla: exchange_rates
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exchange_rates (
                                              id BIGSERIAL PRIMARY KEY,
                                              from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,
    usd_rate_type VARCHAR(20),
    rate NUMERIC(20, 10) NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_exchange_rates_from_to_type
    UNIQUE (from_currency, to_currency, usd_rate_type)
    );

CREATE INDEX IF NOT EXISTS idx_exchange_rates_from_to
    ON exchange_rates(from_currency, to_currency);

-- ------------------------------------------------------------------------------
-- 3. Tabla: additional_services
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS additional_services (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   accommodation_id BIGINT NOT NULL,
                                                   name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    base_price NUMERIC(12, 2) NOT NULL,
    price_type VARCHAR(20) NOT NULL DEFAULT 'TOTAL',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_additional_services_accommodation
    FOREIGN KEY (accommodation_id) REFERENCES accommodations(id) ON DELETE CASCADE,
    CONSTRAINT chk_additional_services_price_type
    CHECK (price_type IN ('PER_DAY', 'TOTAL'))
    );

CREATE INDEX IF NOT EXISTS idx_additional_services_accommodation
    ON additional_services(accommodation_id);

-- ------------------------------------------------------------------------------
-- 4. Tabla: additional_service_images
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS additional_service_images (
                                                         id BIGSERIAL PRIMARY KEY,
                                                         additional_service_id BIGINT NOT NULL,
                                                         image_url VARCHAR(2048) NOT NULL,
    cloudinary_public_id VARCHAR(500) NOT NULL,
    cloudinary_folder VARCHAR(255),
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(50),
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_additional_service_images_service
    FOREIGN KEY (additional_service_id) REFERENCES additional_services(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_additional_service_images_service
    ON additional_service_images(additional_service_id);

-- ------------------------------------------------------------------------------
-- 5. Modificación de tabla: accommodations (moneda base)
-- ------------------------------------------------------------------------------
ALTER TABLE accommodations
    ADD COLUMN IF NOT EXISTS base_currency VARCHAR(10) NOT NULL DEFAULT 'ARS';

-- ------------------------------------------------------------------------------
-- 6. Modificación de tabla: accommodation_payment_methods (moneda del método)
-- ------------------------------------------------------------------------------
ALTER TABLE accommodation_payment_methods
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'ARS';

-- ------------------------------------------------------------------------------
-- 7. Datos iniciales: Tasas de cambio de ejemplo (Seed Data)
--    Serán sobrescritas automáticamente por ExchangeRateProviderService.
-- ------------------------------------------------------------------------------
INSERT INTO exchange_rates (from_currency, to_currency, usd_rate_type, rate, fetched_at)
VALUES
    ('ARS', 'USD', 'BLUE',    0.0008547009, NOW()),
    ('ARS', 'USD', 'OFICIAL', 0.0009803922, NOW()),
    ('ARS', 'USD', 'MEP',     0.0008658009, NOW()),
    ('ARS', 'USD', 'CRYPTO',  0.0008474576, NOW()),
    ('ARS', 'EUR', NULL,      0.0000007560, NOW())
    ON CONFLICT (from_currency, to_currency, usd_rate_type) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 8. Comentarios de documentación
-- ------------------------------------------------------------------------------
COMMENT ON TABLE accepted_currencies IS
'Monedas habilitadas por alojamiento. ARS siempre está habilitado por defecto.';
COMMENT ON TABLE exchange_rates IS
'Caché de tasas de cambio obtenidas desde DolarAPI.com y open.er-api.com.';
COMMENT ON TABLE additional_services IS
'Servicios adicionales ofrecidos por el alojamiento (desayuno, transfer, etc.).';
COMMENT ON TABLE additional_service_images IS
'Imágenes de servicios adicionales almacenadas en Cloudinary (máx. 3 por servicio).';
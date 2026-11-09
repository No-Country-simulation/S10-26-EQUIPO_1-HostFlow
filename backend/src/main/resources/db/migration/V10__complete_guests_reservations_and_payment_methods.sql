-- ==============================================================================
-- HostFlow - Migración V10: Completar esquema de huéspedes, reservas y métodos
--            de pago del huésped.
-- ==============================================================================
-- Propósito: Agregar columnas faltantes a guests y crear las tablas
--            guest_payment_methods, reservations y reservation_links que
--            las entidades JPA esperan. Sin esto, ddl-auto=validate falla
--            en producción.
--
-- Autor: HostFlow Team
-- Versión: 1.0.0
-- Base de Datos: PostgreSQL
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Ampliación de tabla: guests
--    Agrega columnas que la entidad Guest.java declara pero que no estaban
--    en V8 (preferences y la relación con guest_allergies).
-- ------------------------------------------------------------------------------
ALTER TABLE guests
    ADD COLUMN IF NOT EXISTS preferences VARCHAR(1000);

-- Tabla de alergias (ElementCollection de Guest.allergies)
CREATE TABLE IF NOT EXISTS guest_allergies (
                                               guest_id BIGINT NOT NULL,
                                               allergy_name VARCHAR(100),
    CONSTRAINT fk_guest_allergies_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_guest_allergies_guest
    ON guest_allergies(guest_id);

COMMENT ON TABLE guest_allergies IS
'Alergias/restricciones alimentarias del huésped (ElementCollection JPA).';

-- ------------------------------------------------------------------------------
-- 2. Tabla: guest_payment_methods
--    Métodos de pago registrados por cada huésped.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS guest_payment_methods (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     guest_id BIGINT NOT NULL,
                                                     payment_type VARCHAR(30) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    provider_name VARCHAR(100),
    account_number VARCHAR(100),
    account_holder VARCHAR(200),
    expiration_date VARCHAR(10),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guest_payment_methods_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_guest_payment_methods_guest
    ON guest_payment_methods(guest_id);

CREATE INDEX IF NOT EXISTS idx_guest_payment_methods_default
    ON guest_payment_methods(guest_id, is_default);

COMMENT ON TABLE guest_payment_methods IS
'Métodos de pago registrados por un huésped (tarjetas, billeteras, etc.).';

-- ------------------------------------------------------------------------------
-- 3. Tabla: reservations
--    Reservas de alojamiento vinculadas a huéspedes.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
                                            id BIGSERIAL PRIMARY KEY,
                                            confirmation_code VARCHAR(20) NOT NULL UNIQUE,
    accommodation_id BIGINT NOT NULL,
    guest_id BIGINT NOT NULL,
    payment_method_id BIGINT,
    check_in_date TIMESTAMP NOT NULL,
    check_out_date TIMESTAMP NOT NULL,
    nights INTEGER NOT NULL,
    price_per_night NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    taxes NUMERIC(12,2) DEFAULT 0,
    total_price NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    number_of_guests INTEGER NOT NULL DEFAULT 1,
    special_requests TEXT,
    cancellation_reason VARCHAR(500),
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservations_accommodation
    FOREIGN KEY (accommodation_id) REFERENCES accommodations(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservations_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservations_payment_method
    FOREIGN KEY (payment_method_id) REFERENCES guest_payment_methods(id)
    );

CREATE INDEX IF NOT EXISTS idx_reservations_accommodation
    ON reservations(accommodation_id);

CREATE INDEX IF NOT EXISTS idx_reservations_guest
    ON reservations(guest_id);

CREATE INDEX IF NOT EXISTS idx_reservations_status
    ON reservations(status);

CREATE INDEX IF NOT EXISTS idx_reservations_dates
    ON reservations(check_in_date, check_out_date);

CREATE INDEX IF NOT EXISTS idx_reservations_confirmation_code
    ON reservations(confirmation_code);

COMMENT ON TABLE reservations IS
'Reservas de alojamiento con estado, importes y vínculo al huésped.';

-- ------------------------------------------------------------------------------
-- 4. Tabla: reservation_links
--    Enlace/token de confirmación de una reserva (1:1).
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation_links (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 reservation_id BIGINT NOT NULL UNIQUE,
                                                 token VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    reactivation_count INTEGER NOT NULL DEFAULT 0,
    last_reactivated_at TIMESTAMP,
    confirmation_url VARCHAR(2048),
    CONSTRAINT fk_reservation_links_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_reservation_links_token
    ON reservation_links(token);

CREATE INDEX IF NOT EXISTS idx_reservation_links_reservation
    ON reservation_links(reservation_id);

CREATE INDEX IF NOT EXISTS idx_reservation_links_expires
    ON reservation_links(expires_at);

COMMENT ON TABLE reservation_links IS
'Enlace único de confirmación de reserva (token UUID, un solo uso).';
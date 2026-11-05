-- ============================================================================
-- Migración V10: Creación de tablas para el sistema de reservas
-- ============================================================================
-- Esta migración crea las tablas necesarias para gestionar:
-- - Huéspedes (guests)
-- - Métodos de pago de huéspedes (guest_payment_methods)
-- - Reservas (reservations)
-- - Enlaces de confirmación (reservation_links)
--
-- @author HostFlow Team
-- @version 1.0.0
-- ============================================================================

-- ============================================================================
-- 1. TABLA: guests (Huéspedes)
-- ============================================================================
CREATE TABLE IF NOT EXISTS guests (
                                      id BIGSERIAL PRIMARY KEY,
                                      full_name VARCHAR(200) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(40),
    document_number VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Índice para búsquedas rápidas por email
CREATE INDEX IF NOT EXISTS idx_guests_email ON guests(email);

-- Índice para búsquedas por documento
CREATE INDEX IF NOT EXISTS idx_guests_document_number ON guests(document_number);

-- ============================================================================
-- 2. TABLA: guest_payment_methods (Métodos de pago de huéspedes)
-- ============================================================================
CREATE TABLE IF NOT EXISTS guest_payment_methods (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     guest_id BIGINT NOT NULL,
                                                     payment_type VARCHAR(50) NOT NULL,
    provider_name VARCHAR(150),
    account_holder VARCHAR(200),
    account_number VARCHAR(100),
    cbu_cvu VARCHAR(100),
    alias VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guest_payment_methods_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE
    );

-- Índice para búsquedas por huésped
CREATE INDEX IF NOT EXISTS idx_guest_payment_methods_guest ON guest_payment_methods(guest_id);

-- ============================================================================
-- 3. TABLA: reservations (Reservas)
-- ============================================================================
CREATE TABLE IF NOT EXISTS reservations (
                                            id BIGSERIAL PRIMARY KEY,
                                            guest_id BIGINT NOT NULL,
                                            accommodation_id BIGINT NOT NULL,
                                            check_in_date DATE NOT NULL,
                                            check_out_date DATE NOT NULL,
                                            number_of_guests INTEGER NOT NULL,
                                            total_price NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method_id BIGINT,
    special_requests TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    CONSTRAINT fk_reservations_guest
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_accommodation
    FOREIGN KEY (accommodation_id) REFERENCES accommodations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_payment_method
    FOREIGN KEY (payment_method_id) REFERENCES guest_payment_methods(id) ON DELETE SET NULL,
    CONSTRAINT chk_reservation_dates
    CHECK (check_out_date > check_in_date),
    CONSTRAINT chk_number_of_guests
    CHECK (number_of_guests > 0)
    );

-- Índices para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_reservations_guest ON reservations(guest_id);
CREATE INDEX IF NOT EXISTS idx_reservations_accommodation ON reservations(accommodation_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_reservations_dates ON reservations(check_in_date, check_out_date);

-- ============================================================================
-- 4. TABLA: reservation_links (Enlaces de confirmación)
-- ============================================================================
CREATE TABLE IF NOT EXISTS reservation_links (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 reservation_id BIGINT NOT NULL,
                                                 token UUID NOT NULL UNIQUE,
                                                 link_type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_links_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    CONSTRAINT chk_expiration
    CHECK (expires_at > created_at)
    );

-- Índice para búsquedas por token
CREATE INDEX IF NOT EXISTS idx_reservation_links_token ON reservation_links(token);

-- Índice para búsquedas por reserva
CREATE INDEX IF NOT EXISTS idx_reservation_links_reservation ON reservation_links(reservation_id);

-- ============================================================================
-- COMENTARIOS EN LAS TABLAS (Documentación)
-- ============================================================================
COMMENT ON TABLE guests IS 'Huéspedes que realizan reservas en la plataforma';
COMMENT ON TABLE guest_payment_methods IS 'Métodos de pago registrados por los huéspedes';
COMMENT ON TABLE reservations IS 'Reservas de alojamiento realizadas por huéspedes';
COMMENT ON TABLE reservation_links IS 'Enlaces únicos para confirmación/cancelación de reservas';
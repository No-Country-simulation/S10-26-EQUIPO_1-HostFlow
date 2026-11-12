-- ==============================================================================
-- HostFlow - Migración V12: Alinear esquema de reservas al modelo actual
-- ==============================================================================
-- Contexto:
--   V8/V10 crearon un esquema orientado al flujo antiguo (guest obligatorio,
--   payment_method del huésped, sin onboarding embebido ni payment_status).
--   El código actual (reestructuración) espera columnas y tablas adicionales.
--
-- Esta migración es idempotente (IF EXISTS / IF NOT EXISTS) y sirve tanto
-- para BD vacías (tras V1–V11) como con datos de prueba.
--
-- NO modifica: accommodation_payment_methods, additional_services (catálogo),
--              accepted_currencies, exchange_rates.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. reservations: columnas del flujo actual
-- ------------------------------------------------------------------------------

-- Contacto del huésped denormalizado (la reserva PENDING aún no tiene guest_id)
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS guest_name VARCHAR(200);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS guest_email VARCHAR(150);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS guest_phone VARCHAR(40);

-- Moneda e importes de servicios adicionales
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS currency VARCHAR(10);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS additional_services_total NUMERIC(12,2);

-- Pago (separado del status de la reserva)
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS payment_marked_by_owner_id BIGINT;

-- Onboarding
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS boat_transfer_requested BOOLEAN;

-- Backfill seguro para filas existentes (BD con datos)
UPDATE reservations r
SET guest_name = COALESCE(
        NULLIF(TRIM(r.guest_name), ''),
        (SELECT g.full_name FROM guests g WHERE g.id = r.guest_id),
        'Pendiente'
    ),
    guest_email = COALESCE(
        NULLIF(TRIM(r.guest_email), ''),
        (SELECT g.email FROM guests g WHERE g.id = r.guest_id),
        'pendiente@hostflow.local'
    )
WHERE r.guest_name IS NULL OR r.guest_email IS NULL;

UPDATE reservations
SET currency = COALESCE(currency, 'ARS'),
    additional_services_total = COALESCE(additional_services_total, 0),
    payment_status = COALESCE(payment_status, 'PENDING'),
    boat_transfer_requested = COALESCE(boat_transfer_requested, FALSE)
WHERE currency IS NULL
   OR additional_services_total IS NULL
   OR payment_status IS NULL
   OR boat_transfer_requested IS NULL;

-- NOT NULL alineado al modelo (tras backfill)
ALTER TABLE reservations ALTER COLUMN guest_name SET DEFAULT 'Pendiente';
ALTER TABLE reservations ALTER COLUMN guest_email SET DEFAULT 'pendiente@hostflow.local';
ALTER TABLE reservations ALTER COLUMN currency SET DEFAULT 'ARS';
ALTER TABLE reservations ALTER COLUMN additional_services_total SET DEFAULT 0;
ALTER TABLE reservations ALTER COLUMN payment_status SET DEFAULT 'PENDING';
ALTER TABLE reservations ALTER COLUMN boat_transfer_requested SET DEFAULT FALSE;

ALTER TABLE reservations ALTER COLUMN guest_name SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN guest_email SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN currency SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN additional_services_total SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN payment_status SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN boat_transfer_requested SET NOT NULL;

-- guest_id opcional: la reserva se crea antes del onboarding
ALTER TABLE reservations ALTER COLUMN guest_id DROP NOT NULL;

-- FK owner que marcó el pago (solo si la columna existe y aún no hay constraint)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_reservations_payment_marked_by_owner'
    ) THEN
        ALTER TABLE reservations
            ADD CONSTRAINT fk_reservations_payment_marked_by_owner
            FOREIGN KEY (payment_marked_by_owner_id) REFERENCES owners(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_reservations_payment_status
    ON reservations(payment_status);

COMMENT ON COLUMN reservations.guest_name IS
'Nombre de contacto al crear la reserva (antes o sin registro Guest).';
COMMENT ON COLUMN reservations.payment_status IS
'PENDING | PAID. Independiente de status (CONFIRMED/COMPLETED/...).';
COMMENT ON COLUMN reservations.boat_transfer_requested IS
'Indicador de solicitud de transfer; no almacena itinerarios.';

-- ------------------------------------------------------------------------------
-- 2. reservation_links: historial de enlaces + invalidated_at
-- ------------------------------------------------------------------------------
-- El flujo actual puede invalidar enlaces y crear uno nuevo (OneToMany lógico).
-- V10 puso UNIQUE(reservation_id); hay que quitarlo.
ALTER TABLE reservation_links DROP CONSTRAINT IF EXISTS reservation_links_reservation_id_key;

-- Por si el unique se creó como índice con otro nombre
DROP INDEX IF EXISTS reservation_links_reservation_id_key;

ALTER TABLE reservation_links ADD COLUMN IF NOT EXISTS invalidated_at TIMESTAMP;

COMMENT ON COLUMN reservation_links.invalidated_at IS
'Marca de invalidación al renovar o cancelar; el token deja de ser válido.';

-- ------------------------------------------------------------------------------
-- 3. reservation_guest_info (datos de onboarding por estadía)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation_guest_info (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(40) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    preferences VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reservation_guest_info_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_reservation_guest_info_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reservation_guest_allergies (
    reservation_guest_info_id BIGINT NOT NULL,
    allergy_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_reservation_guest_allergies
        PRIMARY KEY (reservation_guest_info_id, allergy_name),
    CONSTRAINT fk_reservation_guest_allergies_info
        FOREIGN KEY (reservation_guest_info_id) REFERENCES reservation_guest_info(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reservation_guest_allergies_info
    ON reservation_guest_allergies(reservation_guest_info_id);

COMMENT ON TABLE reservation_guest_info IS
'Datos personales y preferencias del huésped para una reserva concreta (onboarding).';

-- ------------------------------------------------------------------------------
-- 4. reservation_additional_services (líneas contratadas en la reserva)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation_additional_services (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    additional_service_id BIGINT,
    service_name_snapshot VARCHAR(150) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(12,2) NOT NULL,
    price_type VARCHAR(20) NOT NULL,
    nights_applied INTEGER,
    line_total NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'ARS',
    CONSTRAINT fk_reservation_additional_services_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_additional_services_catalog
        FOREIGN KEY (additional_service_id) REFERENCES additional_services(id)
);

CREATE INDEX IF NOT EXISTS idx_reservation_additional_services_reservation
    ON reservation_additional_services(reservation_id);

COMMENT ON TABLE reservation_additional_services IS
'Servicios adicionales contratados en una reserva (precio histórico aplicado).';

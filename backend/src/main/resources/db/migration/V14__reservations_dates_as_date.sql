-- ==============================================================================
-- HostFlow - Migración V14: check_in_date / check_out_date como DATE
-- ==============================================================================
-- Hibernate mapea LocalDate -> DATE. V10 creó TIMESTAMP; validate falla con:
--   found [timestamp], expecting [date]
-- Conversión segura: USING columna::date (trunca hora; reservas son por noches).
-- Idempotente si ya son date.
-- ==============================================================================

DO $$
BEGIN
    -- check_in_date
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'reservations'
          AND column_name = 'check_in_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE reservations
            ALTER COLUMN check_in_date TYPE DATE
            USING check_in_date::date;
    END IF;

    -- check_out_date
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'reservations'
          AND column_name = 'check_out_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE reservations
            ALTER COLUMN check_out_date TYPE DATE
            USING check_out_date::date;
    END IF;
END $$;

COMMENT ON COLUMN reservations.check_in_date IS 'Fecha de ingreso (solo día; LocalDate).';
COMMENT ON COLUMN reservations.check_out_date IS 'Fecha de salida (solo día; LocalDate).';

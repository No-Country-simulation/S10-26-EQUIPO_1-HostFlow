-- ==============================================================================
-- HostFlow - Migración V15: email de guests deja de ser UNIQUE
-- ==============================================================================
-- Producto:
--   - El correo es dato de contacto y puede repetirse entre huéspedes/reservas.
--   - La identidad reutilizable del huésped es el documento (document_number).
--   - ReservationService.resolveGuest busca solo por documento; mismo documento
--     + mismo email actualiza el registro; no se bloquea por email.
--
-- V8 creó: email VARCHAR(150) NOT NULL UNIQUE
-- Esta migración elimina esa restricción de forma segura (IF EXISTS).
--
-- NO se elimina la unicidad de document_number:
--   - Índice parcial idx_guests_document_number_unique / uk_guests_document
--   - Permite que un cliente vuelva a reservar y se reutilice el mismo Guest.
-- ==============================================================================

-- Constraint implícita de UNIQUE en columna email (PostgreSQL: {table}_{column}_key)
ALTER TABLE guests DROP CONSTRAINT IF EXISTS guests_email_key;

-- Por si en algún entorno se nombró de otra forma
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'guests'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) ILIKE '%email%'
    LOOP
        EXECUTE format('ALTER TABLE guests DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

-- Índice no único para búsquedas por email (ya existía en V8; IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_guests_email ON guests(email);

COMMENT ON COLUMN guests.email IS
'Correo de contacto del huésped. No es único: puede repetirse entre personas o reservas.';
COMMENT ON COLUMN guests.document_number IS
'Documento de identidad. Único cuando no es NULL; se reutiliza el Guest en reservas posteriores.';

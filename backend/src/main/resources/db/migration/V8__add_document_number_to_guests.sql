-- ============================================================================
-- Migración V8: Agregar campo document_number a la tabla guests (Idempotente)
-- ============================================================================
-- Esta migración es segura de ejecutar múltiples veces.
-- Si la columna ya existe (por ejemplo, creada previamente por Hibernate),
-- la omitirá y solo se asegurará de que el índice y el comentario existan.
-- ============================================================================

-- 1. Agregar la columna solo si NO existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'guests'
          AND column_name = 'document_number'
    ) THEN
ALTER TABLE guests ADD COLUMN document_number VARCHAR(20);
END IF;
END $$;

-- 2. Agregar el índice único solo si NO existe
CREATE UNIQUE INDEX IF NOT EXISTS idx_guests_document_number_unique
    ON guests(document_number)
    WHERE document_number IS NOT NULL;

-- 3. Agregar comentario en la columna (esta operación es segura de ejecutar siempre)
COMMENT ON COLUMN guests.document_number IS
'Número de documento de identidad (DNI, CC, pasaporte). Solo números, 5-12 dígitos.';
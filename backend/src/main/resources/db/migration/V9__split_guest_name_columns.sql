-- ==============================================================================
-- Migración V9: Dividir la columna full_name en first_name y last_name
-- ==============================================================================

-- 1. Agregar las nuevas columnas si no existen
ALTER TABLE guests
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

-- 2. Migrar los datos existentes de full_name a first_name y last_name
-- Usamos un bloque anónimo de PostgreSQL (DO) para verificar si la columna
-- 'full_name' existe antes de intentar leerla, evitando el error "columna no existe".
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'guests' AND column_name = 'full_name'
    ) THEN
UPDATE guests
SET
    first_name = TRIM(SPLIT_PART(full_name, ' ', 1)),
    last_name = TRIM(SUBSTRING(full_name FROM POSITION(' ' IN full_name)))
WHERE full_name IS NOT NULL AND POSITION(' ' IN full_name) > 0;
END IF;
END $$;

-- 3. (Opcional) Eliminar la columna full_name si ya no se necesita en el futuro
-- ALTER TABLE guests DROP COLUMN IF EXISTS full_name;
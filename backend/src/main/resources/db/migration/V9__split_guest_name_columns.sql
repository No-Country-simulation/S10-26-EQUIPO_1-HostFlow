-- ============================================================================
-- Migración V9: Separar full_name en first_name y last_name en la tabla guests
-- ============================================================================
-- Esta migración actualiza la estructura de la tabla 'guests' para coincidir
-- con la entidad Guest, que ahora usa firstName y lastName en lugar de fullName.

-- 1. Agregar las nuevas columnas
ALTER TABLE guests
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

-- 2. Migrar los datos existentes de full_name a first_name y last_name
-- Asumimos que el primer espacio separa el nombre del apellido.
UPDATE guests
SET
    first_name = TRIM(SPLIT_PART(full_name, ' ', 1)),
    last_name = TRIM(SUBSTRING(full_name FROM POSITION(' ' IN full_name)))
WHERE full_name IS NOT NULL AND POSITION(' ' IN full_name) > 0;

-- Para los registros que no tienen espacio (solo un nombre), lo ponemos todo en first_name
UPDATE guests
SET
    first_name = full_name,
    last_name = 'N/A'
WHERE full_name IS NOT NULL AND POSITION(' ' IN full_name) = 0;

-- 3. Asegurar que no haya nulos antes de hacerlas NOT NULL
UPDATE guests SET first_name = 'Desconocido' WHERE first_name IS NULL;
UPDATE guests SET last_name = 'Desconocido' WHERE last_name IS NULL;

-- 4. Hacer las nuevas columnas obligatorias (NOT NULL)
ALTER TABLE guests
    ALTER COLUMN first_name SET NOT NULL,
ALTER COLUMN last_name SET NOT NULL;

-- 5. Eliminar la columna antigua full_name
ALTER TABLE guests DROP COLUMN IF EXISTS full_name;

-- 6. Actualizar índices
DROP INDEX IF EXISTS idx_guests_full_name;
CREATE INDEX IF NOT EXISTS idx_guests_name ON guests(first_name, last_name);
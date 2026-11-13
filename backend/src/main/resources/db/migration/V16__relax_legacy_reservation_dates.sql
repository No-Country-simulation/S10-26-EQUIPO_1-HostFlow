-- The current Reservation entity persists dates in check_in_date/check_out_date.
-- Some existing databases still have the legacy check_in/check_out columns
-- with NOT NULL constraints. Hibernate does not remove those constraints when
-- updating the schema, so new inserts fail even though the current date columns
-- are populated. Keep legacy data, but allow new rows to omit the old columns.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'reservations'
          AND column_name = 'check_in'
    ) THEN
        ALTER TABLE reservations ALTER COLUMN check_in DROP NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'reservations'
          AND column_name = 'check_out'
    ) THEN
        ALTER TABLE reservations ALTER COLUMN check_out DROP NOT NULL;
    END IF;
END $$;

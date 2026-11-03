-- Separa los datos de cobro del perfil general y conserva cualquier dato existente.
CREATE TABLE IF NOT EXISTS accommodation_payment_methods (
    id BIGSERIAL PRIMARY KEY,
    accommodation_id BIGINT NOT NULL,
    payment_type VARCHAR(30) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    provider_name VARCHAR(150),
    bank_name VARCHAR(150),
    account_holder VARCHAR(200),
    account_number VARCHAR(100),
    cbu_cvu VARCHAR(100),
    alias VARCHAR(100),
    tax_id VARCHAR(30),
    instructions TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_methods_accommodation
        FOREIGN KEY (accommodation_id) REFERENCES accommodations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payment_methods_accommodation
    ON accommodation_payment_methods(accommodation_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_methods_one_default_per_accommodation
    ON accommodation_payment_methods(accommodation_id) WHERE is_default = TRUE;

/*-- Migra datos bancarios heredados a un método por alojamiento, si hay alguno informado.
INSERT INTO accommodation_payment_methods (
    accommodation_id, payment_type, display_name, provider_name, bank_name,
    account_holder, account_number, cbu_cvu, alias, tax_id,
    is_default, is_active, created_at, updated_at
)
SELECT a.id,
       CASE WHEN a.bank_name ILIKE '%mercado pago%' OR a.bank_name ILIKE '%uala%'
            THEN 'DIGITAL_WALLET' ELSE 'BANK_TRANSFER' END,
       COALESCE(NULLIF(BTRIM(a.bank_name), ''), 'Cuenta bancaria'),
       a.bank_name, a.bank_name, a.bank_account_holder, a.bank_account_number,
       a.bank_cbu_cvu, a.bank_alias, a.bank_cuit,
       TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM accommodations a
WHERE a.bank_name IS NOT NULL OR a.bank_account_holder IS NOT NULL
   OR a.bank_account_number IS NOT NULL OR a.bank_cbu_cvu IS NOT NULL
   OR a.bank_alias IS NOT NULL OR a.bank_cuit IS NOT NULL;

ALTER TABLE accommodations DROP COLUMN IF EXISTS bank_name;
ALTER TABLE accommodations DROP COLUMN IF EXISTS bank_account_holder;
ALTER TABLE accommodations DROP COLUMN IF EXISTS bank_account_number;
ALTER TABLE accommodations DROP COLUMN IF EXISTS bank_cbu_cvu;
ALTER TABLE accommodations DROP COLUMN IF EXISTS bank_alias;
ALTER TABLE accommodations DROP COLUMN IF EXISTS bank_cuit;*/
-- La estructura heredada puede variar entre bases creadas con distintas versiones de JPA/SQL.
-- Solo migramos y eliminamos las columnas bancarias cuando están presentes TODAS.
-- Si el esquema es parcial, conservamos las columnas y sus datos para revisión manual.
DO $$
DECLARE
    legacy_columns_count INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO legacy_columns_count
      FROM information_schema.columns
     WHERE table_schema = current_schema()
       AND table_name = 'accommodations'
       AND column_name IN (
           'bank_name', 'bank_account_holder', 'bank_account_number',
           'bank_cbu_cvu', 'bank_alias', 'bank_cuit'
       );

    IF legacy_columns_count = 6 THEN
        INSERT INTO accommodation_payment_methods (
            accommodation_id, payment_type, display_name, provider_name, bank_name,
            account_holder, account_number, cbu_cvu, alias, tax_id,
            is_default, is_active, created_at, updated_at
        )
        SELECT a.id,
               CASE WHEN a.bank_name ILIKE '%mercado pago%' OR a.bank_name ILIKE '%uala%'
                    THEN 'DIGITAL_WALLET' ELSE 'BANK_TRANSFER' END,
               COALESCE(NULLIF(BTRIM(a.bank_name), ''), 'Cuenta bancaria'),
               a.bank_name, a.bank_name, a.bank_account_holder, a.bank_account_number,
               a.bank_cbu_cvu, a.bank_alias, a.bank_cuit,
               TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
          FROM accommodations a
         WHERE a.bank_name IS NOT NULL OR a.bank_account_holder IS NOT NULL
            OR a.bank_account_number IS NOT NULL OR a.bank_cbu_cvu IS NOT NULL
            OR a.bank_alias IS NOT NULL OR a.bank_cuit IS NOT NULL;

        ALTER TABLE accommodations DROP COLUMN bank_name;
        ALTER TABLE accommodations DROP COLUMN bank_account_holder;
        ALTER TABLE accommodations DROP COLUMN bank_account_number;
        ALTER TABLE accommodations DROP COLUMN bank_cbu_cvu;
        ALTER TABLE accommodations DROP COLUMN bank_alias;
        ALTER TABLE accommodations DROP COLUMN bank_cuit;
    ELSIF legacy_columns_count = 0 THEN
        RAISE NOTICE 'No existen columnas bancarias heredadas en accommodations; no hay datos que migrar.';
    ELSE
        RAISE NOTICE 'Se encontraron % de 6 columnas bancarias heredadas. Se conservan sin migrar ni eliminar para evitar pérdida de datos.', legacy_columns_count;
    END IF;
END $$;

COMMENT ON TABLE accommodation_payment_methods IS 'Métodos de pago configurados por cada alojamiento; un alojamiento puede tener varios.';
COMMENT ON COLUMN accommodation_payment_methods.payment_type IS 'BANK_TRANSFER, DIGITAL_WALLET, CASH, CARD u OTHER.';
COMMENT ON COLUMN accommodation_payment_methods.is_default IS 'Indica el método de pago predeterminado del alojamiento.';

-- Campos específicos por tipo de método de pago.
-- Permanecen nullable porque no aplican a todos los tipos.
ALTER TABLE accommodation_payment_methods
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS user_identifier VARCHAR(200),
    ADD COLUMN IF NOT EXISTS wallet_phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS wallet_email VARCHAR(254);

COMMENT ON COLUMN accommodation_payment_methods.account_type IS 'Tipo de cuenta bancaria; requerido para BANK_TRANSFER.';
COMMENT ON COLUMN accommodation_payment_methods.user_identifier IS 'Nombre de usuario o ID de usuario; requerido para DIGITAL_WALLET.';
COMMENT ON COLUMN accommodation_payment_methods.wallet_phone IS 'Teléfono de la billetera; requerido para DIGITAL_WALLET.';
COMMENT ON COLUMN accommodation_payment_methods.wallet_email IS 'Correo de la billetera; requerido para DIGITAL_WALLET.';

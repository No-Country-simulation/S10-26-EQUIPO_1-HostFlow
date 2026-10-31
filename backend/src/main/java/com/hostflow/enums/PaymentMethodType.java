package com.hostflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo general del método de pago.")
public enum PaymentMethodType {
    BANK_TRANSFER,
    DIGITAL_WALLET,
    CASH,
    CARD,
    OTHER
}

// Opciones predefinidas para agregar fácilmente un método
export const PRESET_PROVIDERS = [
  {
    type: "WALLET",
    label: "Mercado Pago",
    defaultKeys: { alias: "hostflow.mp", cvu: "", publicKey: "", accessToken: "" },
    notes: "Transferir al Alias/CVU o pagar con dinero en cuenta de Mercado Pago.",
  },
  {
    type: "BANK",
    label: "Transferencia Bancaria",
    defaultKeys: { holder: "", cbu: "", alias: "", bank: "" },
    notes: "Enviar comprobante indicando el número de reserva.",
  },
  {
    type: "WALLET",
    label: "PayPal",
    defaultKeys: { email: "", clientId: "", paypalMe: "" },
    notes: "Aceptamos pagos internacionales en USD o EUR.",
  },
  {
    type: "CARD",
    label: "Stripe / Tarjeta",
    defaultKeys: { publishableKey: "", secretKey: "" },
    notes: "Procesamiento con tarjeta de crédito o débito.",
  },
  {
    type: "WALLET",
    label: "Ualá / Billetera digital",
    defaultKeys: { alias: "", cvu: "", holder: "" },
    notes: "Transferir al CVU o Alias y enviar el comprobante.",
  },
];

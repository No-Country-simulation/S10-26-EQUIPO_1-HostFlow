// Formas de pago del alojamiento (máximo MAX_PAYMENT_METHODS por alojamiento).
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const paymentsApi = {
  // El backend devuelve el agregado: { primary, wallets, methods, bankDetails }.
  getPayments: () => request(E.getPayments, { name: "getPayments" }),

  setPrimaryCurrency: (code) =>
    request(E.setPrimaryCurrency, { name: "setPrimaryCurrency", body: { currency: code } }),

  toggleWallet: (code) =>
    request(E.toggleWallet, { name: "toggleWallet", body: { currency: code } }),

  // `enabled` lo aporta la fachada (sale del store).
  togglePaymentMethod: (id, enabled) =>
    request(pathFor(E.togglePaymentMethod, { id }), {
      name: "togglePaymentMethod",
      body: { enabled },
    }),

  addPaymentMethod: (data) =>
    request(E.addPaymentMethod, { name: "addPaymentMethod", body: data }),

  updatePaymentMethod: (id, patch) =>
    request(pathFor(E.updatePaymentMethod, { id }), { name: "updatePaymentMethod", body: patch }),

  removePaymentMethod: (id) =>
    request(pathFor(E.removePaymentMethod, { id }), { name: "removePaymentMethod" }),

  // Datos bancarios del alojamiento (transferencia externa).
  updateBankDetails: (patch) =>
    request(E.updateBankDetails, { name: "updateBankDetails", body: patch }),
}

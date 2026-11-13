// Formas de pago del alojamiento (máximo MAX_PAYMENT_METHODS por alojamiento).
import { ENDPOINTS as E } from "../endpoints"
import { ApiNotConfiguredError } from "../errors"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const paymentsApi = {
  getCurrencies: () => request(E.getCurrencies, { name: "getCurrencies" }),

  upsertCurrency: (configuration) =>
    request(E.upsertCurrency, { name: "upsertCurrency", body: configuration }),

  // El backend devuelve métodos de pago y monedas habilitadas.
  getPayments: async () => {
    const [methodsResult, currenciesResult] = await Promise.all([
      request(E.getPayments, { name: "getPaymentMethods" }),
      request(E.getCurrencies, { name: "getCurrencies" }).catch(() => []),
    ])

    const currencies = Array.isArray(currenciesResult) ? currenciesResult : []
    const enabledCurrencies = currencies.filter((c) => c.enabled).map((c) => c.currency)

    const methods = Array.isArray(methodsResult)
      ? methodsResult.map((m) => ({
          id: String(m.id),
          label: m.displayName || m.providerName || "Método de pago",
          type:
            m.paymentType === "BANK_TRANSFER"
              ? "BANK"
              : m.paymentType === "CARD"
                ? "CARD"
                : "WALLET",
          enabled: m.isActive !== false,
          isDefault: Boolean(m.isDefault),
          bankName: m.bankName || "",
          accountHolder: m.accountHolder || "",
          accountNumber: m.accountNumber || "",
          cbuCvu: m.cbuCvu || "",
          alias: m.alias || "",
          accountType: m.accountType || "",
          keys: {
            holder: m.accountHolder || "",
            alias: m.alias || "",
            cbu: m.cbuCvu || "",
            bank: m.bankName || "",
            email: m.walletEmail || "",
            accountType: m.accountType || "",
            accountNumber: m.accountNumber || "",
            phone: m.walletPhone || "",
            userIdentifier: m.userIdentifier || "",
          },
          notes: m.instructions || "",
        }))
      : []

    return {
      primary: "ARS",
      wallets: enabledCurrencies,
      methods,
      bankDetails: (() => {
        const bank = methods.find((method) => method.type === "BANK")
        return bank ? {
          holder: bank.accountHolder,
          alias: bank.alias,
          cbu: bank.cbuCvu,
          bank: bank.bankName,
          accountType: bank.accountType,
          accountNumber: bank.accountNumber,
          notes: bank.notes,
          id: bank.id,
        } : { id: null, holder: "", alias: "", cbu: "", bank: "", accountType: "", accountNumber: "", notes: "" }
      })(),
    }
  },

  // El backend usa ARS como moneda base y solo permite configurar monedas
  // secundarias. El cambio de moneda base queda en modo local.
  setPrimaryCurrency: () => {
    throw new ApiNotConfiguredError("setPrimaryCurrency")
  },

  toggleWallet: (code, enabled = true) =>
    request(E.toggleWallet, {
      name: "toggleWallet",
      body: { currency: code, enabled, useAutomaticRate: true, usdRateType: code === "USD" ? "BLUE" : undefined },
    }),

  togglePaymentMethod: (id, enabled) =>
    request(pathFor(E.togglePaymentMethod, { id }), {
      name: "togglePaymentMethod",
      body: { isActive: enabled },
    }),

  addPaymentMethod: (data) => {
    const typeMap = {
      TRANSFER: "BANK_TRANSFER",
      BANK: "BANK_TRANSFER",
      CARD: "CARD",
      WALLET: "DIGITAL_WALLET",
    }
    const body = {
      displayName: data.label || data.displayName || "Método de pago",
      providerName: data.providerName || data.label || data.displayName || "Método de pago",
      paymentType: typeMap[data.type] || "DIGITAL_WALLET",
      isActive: data.enabled !== false,
      bankName: data.bankName || data.bank || data.keys?.bank,
      accountHolder: data.accountHolder || data.holder || data.keys?.holder,
      accountNumber: data.accountNumber || data.keys?.accountNumber || data.keys?.cbu,
      cbuCvu: data.cbuCvu || data.cbu || data.keys?.cbu,
      alias: data.alias || data.keys?.alias,
      accountType: data.accountType || data.keys?.accountType,
      userIdentifier: data.userIdentifier || data.keys?.userIdentifier || data.keys?.email || data.keys?.paypalMe || data.keys?.publicKey,
      walletPhone: data.walletPhone || data.keys?.phone,
      walletEmail: data.walletEmail || data.keys?.email,
      instructions: data.instructions || data.notes,
    }
    return request(E.addPaymentMethod, { name: "addPaymentMethod", body })
  },

  updatePaymentMethod: (id, patch) => {
    const typeMap = {
      TRANSFER: "BANK_TRANSFER",
      BANK: "BANK_TRANSFER",
      CARD: "CARD",
      WALLET: "DIGITAL_WALLET",
    }
    const body = {
      ...(patch.displayName || patch.label ? { displayName: patch.displayName || patch.label } : {}),
      ...(patch.providerName || patch.displayName || patch.label ? { providerName: patch.providerName || patch.displayName || patch.label } : {}),
      ...(patch.type ? { paymentType: typeMap[patch.type] || patch.type } : {}),
      ...(patch.enabled !== undefined ? { isActive: patch.enabled } : {}),
      ...((patch.bankName ?? patch.bank ?? patch.keys?.bank) !== undefined ? { bankName: patch.bankName ?? patch.bank ?? patch.keys?.bank } : {}),
      ...((patch.accountHolder ?? patch.holder ?? patch.keys?.holder) !== undefined ? { accountHolder: patch.accountHolder ?? patch.holder ?? patch.keys?.holder } : {}),
      ...((patch.accountNumber ?? patch.keys?.accountNumber ?? patch.keys?.cbu) !== undefined ? { accountNumber: patch.accountNumber ?? patch.keys?.accountNumber ?? patch.keys?.cbu } : {}),
      ...((patch.cbuCvu ?? patch.cbu ?? patch.keys?.cbu) !== undefined ? { cbuCvu: patch.cbuCvu ?? patch.cbu ?? patch.keys?.cbu } : {}),
      ...((patch.alias ?? patch.keys?.alias) !== undefined ? { alias: patch.alias ?? patch.keys?.alias } : {}),
      ...((patch.accountType ?? patch.keys?.accountType) !== undefined ? { accountType: patch.accountType ?? patch.keys?.accountType } : {}),
      ...((patch.userIdentifier ?? patch.keys?.userIdentifier ?? patch.keys?.email ?? patch.keys?.paypalMe ?? patch.keys?.publicKey) !== undefined ? { userIdentifier: patch.userIdentifier ?? patch.keys?.userIdentifier ?? patch.keys?.email ?? patch.keys?.paypalMe ?? patch.keys?.publicKey } : {}),
      ...((patch.walletPhone ?? patch.keys?.phone) !== undefined ? { walletPhone: patch.walletPhone ?? patch.keys?.phone } : {}),
      ...((patch.walletEmail ?? patch.keys?.email) !== undefined ? { walletEmail: patch.walletEmail ?? patch.keys?.email } : {}),
      ...(patch.instructions !== undefined || patch.notes !== undefined ? { instructions: patch.instructions ?? patch.notes } : {}),
    }
    return request(pathFor(E.updatePaymentMethod, { id }), {
      name: "updatePaymentMethod",
      body,
    })
  },

  removePaymentMethod: (id) =>
    request(pathFor(E.removePaymentMethod, { id }), { name: "removePaymentMethod" }),

  // Datos bancarios del alojamiento (transferencia externa).
  updateBankDetails: (patch) => {
    // Si viene un ID de método bancario, lo actualiza
    if (patch.id) {
      return paymentsApi.updatePaymentMethod(patch.id, patch)
    }
    return paymentsApi.addPaymentMethod({
      ...patch,
      label: patch.label || "Transferencia bancaria",
      type: "BANK",
      bankName: patch.bankName || patch.bank,
      accountHolder: patch.accountHolder || patch.holder,
      cbuCvu: patch.cbuCvu || patch.cbu,
      instructions: patch.instructions || patch.notes,
    })
  },
}

// Formas de pago: proyección de la respuesta en el store + fallback local.
import { actions, store } from "../../state/store"
import { MAX_PAYMENT_METHODS } from "../../domain/payments"

const mapPaymentMethod = (method) => ({
  id: String(method.id),
  label: method.displayName || method.providerName || method.label || "Método de pago",
  type: method.paymentType === "BANK_TRANSFER" ? "BANK" : method.paymentType === "CARD" ? "CARD" : "WALLET",
  enabled: method.isActive ?? method.enabled ?? true,
  isDefault: Boolean(method.isDefault),
  keys: {
    holder: method.accountHolder || "",
    alias: method.alias || "",
    cbu: method.cbuCvu || "",
    bank: method.bankName || "",
    accountType: method.accountType || "",
    accountNumber: method.accountNumber || "",
    phone: method.walletPhone || "",
    email: method.walletEmail || "",
  },
  notes: method.instructions || method.notes || "",
})

export const paymentsOps = {
  getPayments: {
    project: (response) => {
      if (response && typeof response === "object") {
        actions.setPayments({
          primary: response.primary,
          wallets: Array.isArray(response.wallets) ? response.wallets : undefined,
          // Límite estricto también del lado del cliente.
          methods: Array.isArray(response.methods)
            ? response.methods.slice(0, MAX_PAYMENT_METHODS)
            : undefined,
          bankDetails: response.bankDetails,
        })
      }
      return store.get().payments
    },
    local: () => store.get().payments || {},
  },

  setPrimaryCurrency: {
    project: (_response, code) => {
      actions.setPrimaryCurrency(code)
      return code
    },
    local: (code) => {
      actions.setPrimaryCurrency(code)
      return code
    },
  },

  toggleWallet: {
    send: (code) => [code, !(store.get().payments.wallets || []).includes(code)],
    project: (_response, code) => {
      actions.toggleWallet(code)
      return store.get().payments.wallets
    },
    local: (code) => {
      actions.toggleWallet(code)
      return store.get().payments.wallets
    },
  },

  togglePaymentMethod: {
    // `enabled` sale del store: es lo que viaja al backend y, si la respuesta
    // no lo trae, lo que queda aplicado.
    send: (id) => [
      id,
      !store.get().payments.methods.find((m) => String(m.id) === String(id))?.enabled,
    ],
    project: (response, id, enabled) => {
      actions.updatePaymentMethod(id, { enabled: response?.enabled ?? enabled })
      return store.get().payments.methods
    },
    local: (id) => {
      actions.togglePaymentMethod(id)
      return store.get().payments.methods
    },
  },

  // Alta respetando el tope de MAX_PAYMENT_METHODS: si ya está lleno no se
  // llama al backend (evita un 400 que la pantalla tendría que traducir).
  addPaymentMethod: {
    send: (data) => {
      if (store.get().payments.methods.length >= MAX_PAYMENT_METHODS) {
        const error = new Error(`Podés configurar hasta ${MAX_PAYMENT_METHODS} métodos de pago.`)
        error.status = 409
        throw error
      }
      return [data]
    },
    project: (created) => {
      const method = mapPaymentMethod(created)
      actions.setPayments({
        methods: [...store.get().payments.methods, method].slice(0, MAX_PAYMENT_METHODS),
      })
      return method
    },
    local: (data) => actions.addPaymentMethod(data),
  },

  updatePaymentMethod: {
    project: (updated, id, patch) => {
      actions.updatePaymentMethod(id, updated ? mapPaymentMethod(updated) : patch)
      return store.get().payments.methods.find((m) => String(m.id) === String(id))
    },
    local: (id, patch) => {
      actions.updatePaymentMethod(id, patch)
      return store.get().payments.methods.find((m) => String(m.id) === String(id))
    },
  },

  removePaymentMethod: {
    project: (_response, id) => {
      actions.removePaymentMethod(id)
      return store.get().payments.methods
    },
    local: (id) => {
      actions.removePaymentMethod(id)
      return store.get().payments.methods
    },
  },

  updateBankDetails: {
    send: (patch) => {
      const current = store.get().payments.methods.find((method) => method.type === "BANK" || method.type === "TRANSFER")
      if (!current && store.get().payments.methods.length >= MAX_PAYMENT_METHODS) {
        const error = new Error(`Podés configurar hasta ${MAX_PAYMENT_METHODS} métodos de pago.`)
        error.status = 409
        throw error
      }
      return [current ? { ...patch, id: current.id } : patch]
    },
    project: (response) => {
      const method = mapPaymentMethod(response)
      const current = store.get().payments.methods
      const exists = current.some((item) => String(item.id) === String(method.id))
      actions.setPayments({
        methods: exists
          ? current.map((item) => String(item.id) === String(method.id) ? method : item)
          : [...current, method].slice(0, MAX_PAYMENT_METHODS),
        bankDetails: {
          id: method.id,
          holder: method.keys.holder,
          alias: method.keys.alias,
          cbu: method.keys.cbu,
          bank: method.keys.bank,
          accountType: method.keys.accountType,
          accountNumber: method.keys.accountNumber,
          notes: method.notes,
        },
      })
      return store.get().payments.bankDetails
    },
    local: (patch) => {
      actions.updateBankDetails(patch)
      return store.get().payments.bankDetails
    },
  },
}

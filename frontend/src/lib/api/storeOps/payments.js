// Formas de pago: proyección de la respuesta en el store + fallback local.
import { actions, store } from "../../state/store"
import { MAX_PAYMENT_METHODS } from "../../domain/payments"

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
      actions.setPayments({
        methods: [...store.get().payments.methods, created].slice(0, MAX_PAYMENT_METHODS),
      })
      return created
    },
    local: (data) => actions.addPaymentMethod(data),
  },

  updatePaymentMethod: {
    project: (updated, id, patch) => {
      actions.updatePaymentMethod(id, updated || patch)
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
    project: (_response, patch) => {
      actions.updateBankDetails(patch)
      return store.get().payments.bankDetails
    },
    local: (patch) => {
      actions.updateBankDetails(patch)
      return store.get().payments.bankDetails
    },
  },
}

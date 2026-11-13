// Onboarding del huésped (B2C): proyección + fallback local.
import { actions, store } from "../../state/store"
import { mergeLodging, toBooking } from "./shared"

export const onboardingOps = {
  getOnboarding: {
    // Hidrata el store con el alojamiento, la reserva, los servicios
    // habilitados y las formas de pago activas, de modo que la ficha (y la
    // copia de credenciales de pago) se vea igual en el celular del huésped
    // que en el panel del anfitrión.
    project: (data, _lodgingId, bookingId) => {
      if (data?.lodging) actions.updateLodging(mergeLodging(data.lodging))
      if (data?.booking) actions.updateBooking(bookingId, toBooking(data.booking))
      if (Array.isArray(data?.services)) actions.setServices(data.services)
      if (data?.payments) {
        actions.setPayments({
          primary: data.payments.primary,
          wallets: data.payments.wallets,
          methods: data.payments.methods,
          bankDetails: data.payments.bankDetails,
        })
      }
      return data
    },
    local: () => null,
  },

  submitOnboarding: {
    // El enlace público trae lodgingId y bookingId; el panel llama solo con el
    // bookingId, así que el alojamiento sale del store.
    send: (id, data) => [store.get().lodging?.id, id, data],
    project: (_response, id, data) => {
      actions.updateBooking(id, {
        guestName: data.guestName,
        guestEmail: data.guestEmail,
        guestPhone: data.guestPhone,
        selectedServices: Array.isArray(data.selectedServices) ? data.selectedServices : [],
        onboarding: { ...data, completedAt: new Date().toISOString() },
      })
      return data
    },
    local: (id, data) => {
      actions.submitOnboarding(id, data)
      return data
    },
  },

  createBoatQuoteRequest: {
    project: (created, id) => {
      actions.updateBooking(id, { boatQuote: created })
      return created
    },
    local: (id, quote) => actions.createBoatQuoteRequest(id, quote),
  },

  declineBoatQuote: {
    project: (_response, id) => actions.declineBoatQuote(id),
    local: (id) => actions.declineBoatQuote(id),
  },
}

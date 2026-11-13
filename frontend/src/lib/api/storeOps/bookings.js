// Reservas: proyección de la respuesta en el store + fallback local.
import { actions, store } from "../../state/store"
import { toBooking } from "./shared"

const backendUnavailable = (operation) => {
  throw new Error(`No se pudo ${operation} en el backend. La interfaz no guardó una reserva local de demostración.`)
}

export const bookingsOps = {
  getBookingById: {
    project: (response) => {
      const booking = toBooking(response)
      const exists = store.get().bookings.some((item) => String(item.id) === String(booking.id))
      actions.setBookings(exists
        ? store.get().bookings.map((item) => String(item.id) === String(booking.id) ? booking : item)
        : [...store.get().bookings, booking])
      return booking
    },
    local: () => backendUnavailable("consultar la reserva"),
  },

  getBookings: {
    project: (response) => {
      if (Array.isArray(response)) actions.setBookings(response.map(toBooking))
      return store.get().bookings
    },
    local: () => {
      actions.setBookings([])
      return backendUnavailable("consultar las reservas")
    },
  },

  createBooking: {
    project: (created) => {
      const booking = toBooking(created)
      actions.setBookings([...store.get().bookings, booking])
      return booking
    },
    local: () => backendUnavailable("crear la reserva"),
  },

  updateBooking: {
    project: (updated, id, patch) => {
      actions.updateBooking(id, updated || patch)
      return store.get().bookings.find((b) => String(b.id) === String(id))
    },
    local: () => backendUnavailable("actualizar la reserva"),
  },

  setBookingStatus: {
    project: (_response, id, status) => {
      actions.setBookingStatus(id, status)
      return status
    },
    local: () => backendUnavailable("actualizar el estado de la reserva"),
  },

  setPaymentStatus: {
    project: (_response, id, paymentStatus) => {
      actions.setPaymentStatus(id, paymentStatus)
      return paymentStatus
    },
    local: () => backendUnavailable("registrar el pago de la reserva"),
  },

  regenerateOnboardingLink: {
    project: (response, id) => {
      const link = response?.confirmationUrl ? response : response?.confirmationLink
      const issuedAt = link?.createdAt || response?.onboardingLinkIssuedAt || new Date().toISOString()
      actions.updateBooking(id, { onboardingLinkIssuedAt: issuedAt, confirmationLink: link })
      return link || issuedAt
    },
    local: () => backendUnavailable("renovar el enlace de llegada"),
  },
}

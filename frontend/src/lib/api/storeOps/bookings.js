// Reservas: proyección de la respuesta en el store + fallback local.
import { actions, store } from "../../state/store"
import { toBooking } from "./shared"

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
    local: (id) => store.get().bookings.find((item) => String(item.id) === String(id)) || null,
  },

  getBookings: {
    project: (response) => {
      if (Array.isArray(response)) actions.setBookings(response.map(toBooking))
      return store.get().bookings
    },
    local: () => store.get().bookings || [],
  },

  createBooking: {
    project: (created) => {
      const booking = toBooking(created)
      actions.setBookings([...store.get().bookings, booking])
      return booking
    },
    local: (data) => actions.createBooking(data),
  },

  updateBooking: {
    project: (updated, id, patch) => {
      actions.updateBooking(id, updated || patch)
      return store.get().bookings.find((b) => String(b.id) === String(id))
    },
    local: (id, patch) => {
      actions.updateBooking(id, patch)
      return store.get().bookings.find((b) => String(b.id) === String(id))
    },
  },

  setBookingStatus: {
    project: (_response, id, status) => {
      actions.setBookingStatus(id, status)
      return status
    },
    local: (id, status) => {
      actions.setBookingStatus(id, status)
      return status
    },
  },

  setPaymentStatus: {
    project: (_response, id, paymentStatus) => {
      actions.setPaymentStatus(id, paymentStatus)
      return paymentStatus
    },
    local: (id, paymentStatus) => {
      actions.setPaymentStatus(id, paymentStatus)
      return paymentStatus
    },
  },

  regenerateOnboardingLink: {
    project: (response, id) => {
      const link = response?.confirmationUrl ? response : response?.confirmationLink
      const issuedAt = link?.createdAt || response?.onboardingLinkIssuedAt || new Date().toISOString()
      actions.updateBooking(id, { onboardingLinkIssuedAt: issuedAt, confirmationLink: link })
      return link || issuedAt
    },
    local: (id) => actions.regenerateOnboardingLink(id),
  },
}

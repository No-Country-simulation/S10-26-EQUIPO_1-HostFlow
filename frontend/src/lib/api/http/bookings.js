// Reservas del alojamiento.
import { ENDPOINTS as E } from "../endpoints"
import { ApiNotConfiguredError } from "../errors"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const bookingsApi = {
  getBookings: () => request(E.getBookings, { name: "getBookings" }),
  getBookingById: (id) => request(pathFor(E.getBookingById, { id }), { name: "getBookingById" }),

  createBooking: (data) => {
    const rawName = String(data.guestName || `${data.guestFirstName || ""} ${data.guestLastName || ""}`).trim()
    const spaceIdx = rawName.indexOf(" ")
    const guestFirstName = data.guestFirstName || (spaceIdx > 0 ? rawName.slice(0, spaceIdx) : rawName)
    const guestLastName = data.guestLastName || (spaceIdx > 0 ? rawName.slice(spaceIdx + 1) : "")

    const body = {
      guestFirstName,
      guestLastName,
      guestEmail: data.guestEmail,
      guestPhone: data.guestPhone || null,
      checkIn: data.checkIn,
      checkOut: data.checkOut,
      guestCount: Number(data.guests ?? data.guestCount ?? 1),
      specialRequests: data.additional || data.specialRequests || "",
    }
    return request(E.createBooking, { name: "createBooking", body })
  },

  updateBooking: () => {
    throw new ApiNotConfiguredError("updateBooking")
  },

  setBookingStatus: async (id, status) => {
    if (status === "CANCELLED") {
      const endpoint = pathFor(E.cancelBooking, { id })
      return request(
        { ...endpoint, path: `${endpoint.path}?reason=Cancelada%20por%20el%20anfitri%C3%B3n` },
        { name: "cancelBooking" },
      )
    }
    if (status === "COMPLETED") {
      return request(pathFor(E.completeBooking, { id }), { name: "completeBooking" })
    }
    // CONFIRMED is set by completing the guest onboarding. PENDING is not a
    // reversible transition in the backend reservation lifecycle.
    throw new ApiNotConfiguredError("setBookingStatus")
  },

  // El pago ocurre fuera de HostFlow; el endpoint solo permite registrar que
  // el propietario recibió el pago. No existe una transición remota a PENDING.
  setPaymentStatus: (id, paymentStatus) => {
    if (paymentStatus !== "PAID") throw new ApiNotConfiguredError("setPaymentStatus")
    return request(pathFor(E.markBookingPaid, { id }), { name: "markBookingPaid" })
  },

  // Renueva el enlace de llegada del huésped: nueva vigencia de 24 h.
  regenerateOnboardingLink: (id) =>
    request(pathFor(E.regenerateOnboardingLink, { id }), {
      name: "regenerateOnboardingLink",
    }),

  checkReservationAvailability: (checkIn, checkOut) =>
    request(E.checkReservationAvailability, {
      name: "checkReservationAvailability",
      query: { checkIn, checkOut },
    }),
}

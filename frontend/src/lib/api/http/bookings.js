// Reservas del alojamiento.
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const bookingsApi = {
  getBookings: () => request(E.getBookings, { name: "getBookings" }),

  createBooking: (data) => request(E.createBooking, { name: "createBooking", body: data }),

  updateBooking: (id, patch) =>
    request(pathFor(E.updateBooking, { id }), { name: "updateBooking", body: patch }),

  setBookingStatus: (id, status) =>
    request(pathFor(E.setBookingStatus, { id }), { name: "setBookingStatus", body: { status } }),

  // Estado del pago de una reserva: { paymentStatus: "PENDING" | "PAID" }.
  setPaymentStatus: (id, paymentStatus) =>
    request(pathFor(E.setPaymentStatus, { id }), {
      name: "setPaymentStatus",
      body: { paymentStatus },
    }),

  // Renueva el enlace de llegada del huésped: nueva vigencia de 24 h.
  regenerateOnboardingLink: (id) =>
    request(pathFor(E.regenerateOnboardingLink, { id }), { name: "regenerateOnboardingLink" }),
}

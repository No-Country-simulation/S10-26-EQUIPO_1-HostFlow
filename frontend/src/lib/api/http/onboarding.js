// Onboarding del huésped (B2C, enlace único).
//
// Lectura y envío del onboarding son públicos: el token de enlace identifica
// la reserva y no requiere una sesión de huésped.
import { ENDPOINTS as E } from "../endpoints"
import { ApiNotConfiguredError } from "../errors"
import { request } from "../httpClient"

export const onboardingApi = {
  getOnboarding: (tokenOrLodgingId, bookingId) => {
    if (bookingId !== undefined) throw new ApiNotConfiguredError("getOnboarding")
    return request(E.getOnboarding, {
      name: "getOnboarding",
      auth: false,
      query: { token: tokenOrLodgingId },
    })
  },

  // El token va como query parameter porque el backend usa un enlace público
  // de un solo uso para leer y completar la ficha.
  submitOnboarding: (token, data) =>
    request(E.submitOnboarding, {
      name: "submitOnboarding",
      auth: false,
      query: { token },
      body: {
        firstName: data.firstName || String(data.guestName || "").split(/\s+/)[0] || "",
        lastName: data.lastName || String(data.guestName || "").split(/\s+/).slice(1).join(" "),
        documentNumber: data.guestDocument || data.documentNumber,
        email: data.guestEmail || data.email,
        phone: data.guestPhone || data.phone,
        allergies: Array.isArray(data.allergies) ? data.allergies : [],
        preferences: data.preferences || "",
        additionalServices: (data.selectedServices || []).map((service) => ({
          serviceId: Number(service.serviceId ?? service.id),
          quantity: Number(service.quantity || 1),
        })),
        boatTransferRequested: Boolean(data.boatTransferRequested ?? data.boatQuoteRequested),
      },
    }),

  // El contrato del backend guarda solo el indicador boatTransferRequested;
  // la cotización detallada del traslado queda en el estado de interfaz.
  createBoatQuoteRequest: () => {
    throw new ApiNotConfiguredError("createBoatQuoteRequest")
  },

  declineBoatQuote: () => {
    throw new ApiNotConfiguredError("declineBoatQuote")
  },
}

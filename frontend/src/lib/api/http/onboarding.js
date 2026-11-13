// Onboarding del huésped (B2C, enlace único).
//
// La lectura es PÚBLICA: el huésped no tiene token, así que getOnboarding va
// sin Authorization. El resto de operaciones sí viajan con el JWT del panel.
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const onboardingApi = {
  getOnboarding: (lodgingId, bookingId) =>
    request(pathFor(E.getOnboarding, { lodgingId, bookingId }), {
      name: "getOnboarding",
      auth: false,
    }),

  // El enlace público trae lodgingId y bookingId; el panel llama solo con el
  // bookingId, así que la fachada aporta el alojamiento (sale del store).
  submitOnboarding: (lodgingId, bookingId, data) =>
    request(pathFor(E.submitOnboarding, { lodgingId, bookingId }), {
      name: "submitOnboarding",
      body: data,
    }),

  // Solicitud de cotización de lancha: genera el JSON; NO lo envía a otra API.
  createBoatQuoteRequest: (id, quote) =>
    request(pathFor(E.createBoatQuoteRequest, { id }), {
      name: "createBoatQuoteRequest",
      body: quote,
    }),

  declineBoatQuote: (id) =>
    request(pathFor(E.declineBoatQuote, { id }), { name: "declineBoatQuote" }),
}

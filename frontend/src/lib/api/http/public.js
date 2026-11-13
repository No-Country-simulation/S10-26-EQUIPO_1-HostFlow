// Rutas públicas para huéspedes y consulta de precios.
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const publicApi = {
  createOrGetGuest: (guest) =>
    request(E.createOrGetGuest, { name: "createOrGetGuest", auth: false, body: guest }),

  getGuestByEmail: (email) =>
    request(pathFor(E.getGuestByEmail, { email }), { name: "getGuestByEmail", auth: false }),

  getPublicBasePrice: (accommodationId) =>
    request(pathFor(E.getPublicBasePrice, { accommodationId }), { name: "getPublicBasePrice", auth: false }),

  getPublicServices: (accommodationId) =>
    request(pathFor(E.getPublicServices, { accommodationId }), { name: "getPublicServices", auth: false }),
}

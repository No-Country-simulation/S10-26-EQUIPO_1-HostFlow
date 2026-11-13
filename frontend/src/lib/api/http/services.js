// Servicios adicionales del alojamiento.
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const servicesApi = {
  getServices: () => request(E.getServices, { name: "getServices" }),

  addService: (data) => request(E.addService, { name: "addService", body: data }),

  updateService: (id, patch) =>
    request(pathFor(E.updateService, { id }), { name: "updateService", body: patch }),

  removeService: (id) => request(pathFor(E.removeService, { id }), { name: "removeService" }),

  toggleService: (id) => request(pathFor(E.toggleService, { id }), { name: "toggleService" }),
}

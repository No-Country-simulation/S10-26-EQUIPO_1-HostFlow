// Dueños de alojamiento (JWT). Auth y Owners son los únicos módulos ya
// implementados en el backend; el listado de dueños se usa además como
// respaldo de lectura de otros módulos pendientes.
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const ownersApi = {
  getOwnerByEmail: (email) =>
    request(pathFor(E.getOwnerByEmail, { email }), { name: "getOwnerByEmail" }),

  getOwnerById: (id) => request(pathFor(E.getOwnerById, { id }), { name: "getOwnerById" }),

  getAllOwners: () => request(E.getAllOwners, { name: "getAllOwners" }),

  // Solo ROLE_ADMIN → ?enabled=true|false
  toggleOwnerStatus: (id, enabled) => {
    const endpoint = pathFor(E.toggleOwnerStatus, { id })
    return request(
      { ...endpoint, path: `${endpoint.path}?enabled=${enabled}` },
      { name: "toggleOwnerStatus" },
    )
  },
}

// Rutas de propietarios (JWT), usadas por el panel y la administración.
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

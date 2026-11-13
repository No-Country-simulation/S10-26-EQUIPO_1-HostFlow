// Módulo de administración (solo ROLE_ADMIN).
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { toAdminLodging } from "../transforms"
import { pathFor } from "./pathFor"

export const adminApi = {
  // Métricas agregadas del panel.
  getAdminStats: async () => {
    const stats = await request(E.getAdminStats, { name: "getAdminStats" })
    return {
      lodgings: Number(stats?.lodgings ?? 0),
      active: Number(stats?.active ?? 0),
      paused: Number(stats?.paused ?? 0),
      hosts: Number(stats?.hosts ?? 0),
      bookings: Number(stats?.bookings ?? 0),
    }
  },

  // Listado de alojamientos: el backend hoy lo expone como listado de dueños.
  getAdminLodgings: async () => {
    const owners = await request(E.getAdminLodgings, { name: "getAllOwners" })
    return Array.isArray(owners) ? owners.map(toAdminLodging) : []
  },

  // Pausa o reactiva un alojamiento habilitando o deshabilitando su dueño.
  setAdminLodgingStatus: async (id, status) => {
    const enabled = status === "ACTIVE"
    const endpoint = pathFor(E.setAdminLodgingStatus, { id })
    const updated = await request(
      { ...endpoint, path: `${endpoint.path}?enabled=${enabled}` },
      { name: "toggleOwnerStatus" },
    )
    return toAdminLodging(updated)
  },
}

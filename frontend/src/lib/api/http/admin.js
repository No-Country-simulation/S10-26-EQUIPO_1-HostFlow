// Módulo de administración (solo ROLE_ADMIN).
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { toAdminLodging } from "../transforms"
import { pathFor } from "./pathFor"

export const adminApi = {
  getOwnerMetrics: () => request(E.getOwnerMetrics, { name: "getOwnerMetrics" }),

  // Métricas agregadas del panel.
  getAdminStats: async () => {
    const [stats, owners] = await Promise.all([
      request(E.getAdminStats, { name: "getAdminStats" }),
      request(E.getAdminLodgings, { name: "getAllOwners" }),
    ])
    const total = Number(stats?.totalAccommodations ?? 0)
    const inactive = Number(stats?.inactiveAccommodations ?? 0)
    return {
      lodgings: total,
      active: Math.max(0, total - inactive),
      paused: inactive,
      hosts: Array.isArray(owners) ? new Set(owners.map((owner) => owner.email).filter(Boolean)).size : 0,
      bookings: Number(stats?.totalCompletedReservations ?? 0),
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

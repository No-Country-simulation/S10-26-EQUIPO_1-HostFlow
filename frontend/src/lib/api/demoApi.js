// Adaptador de DEMOSTRACIÓN (se usa cuando VITE_API_URL NO está definida).
// Implementa el MISMO contrato que httpApi.js, pero en memoria: no valida credenciales
// contra un servidor, no envía nada a la red y no emite tokens reales.
// Las operaciones mutan el store reactivo de src/lib/state/store.js.
import { actions, store } from "../state/store"

export const demoApi = {
  ...actions,
  // Lecturas (getters) en memoria para el modo demostración
  getLodging: async () => store.get().lodging,
  updateLodging: async (patch) => {
    actions.updateLodging(patch)
    return store.get().lodging
  },
  getBlockedDays: async () => store.get().blockedDays || [],
  getBookings: async () => store.get().bookings || [],
  getPayments: async () => store.get().payments || {},
  getServices: async () => store.get().services || [],
  getAdminLodgings: async () => store.get().admin?.lodgings || [],
  // En demo todo ya vive en el store del navegador: no hay nada que traer.
  getOnboarding: async () => null,
  // Métricas agregadas del panel de administración.
  getAdminStats: async () => {
    const lodgings = store.get().admin?.lodgings || []
    return {
      lodgings: lodgings.length,
      active: lodgings.filter((l) => l.status === "ACTIVE").length,
      paused: lodgings.filter((l) => l.status === "PAUSED").length,
      hosts: new Set(lodgings.map((l) => l.ownerEmail)).size,
      bookings: store.get().bookings?.length || 0,
    }
  },
  setAdminLodgingStatus: async (id, status) => {
    actions.setAdminLodgingStatus(id, status)
    return { id, status }
  },
  logout: actions.logout,
  isAuthenticated: () => Boolean(store.get().user),
}

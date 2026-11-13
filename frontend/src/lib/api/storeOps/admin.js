// Módulo de administración (solo ROLE_ADMIN): proyección + fallback local.
import { actions, store } from "../../state/store"
import { buildAdminStats } from "../adminStats"
import { isEndpointUnavailable } from "../errors"
import { httpApi } from "../httpApi"
import { toAdminLodging } from "../transforms"

export const adminOps = {
  getAdminStats: {
    // Sin `project`: el adaptador devuelve las métricas ya armadas.
    // Si /admin/stats todavía no existe se calculan con el listado de dueños
    // que el backend sí expone.
    local: async () => {
      // Se calculan sobre el listado REAL de dueños (no sobre el seed de la
      // demo) para que las métricas coincidan con lo que muestra la tabla.
      let lodgings = store.get().admin?.lodgings || []
      try {
        const owners = await httpApi.getAllOwners()
        if (Array.isArray(owners)) {
          lodgings = owners.map(toAdminLodging)
          actions.setAdminLodgings(lodgings)
        }
      } catch (error) {
        if (!isEndpointUnavailable(error)) throw error
      }
      return buildAdminStats({ lodgings })
    },
  },

  getAdminLodgings: {
    project: (list) => {
      actions.setAdminLodgings(list)
      return list
    },
    local: () => store.get().admin?.lodgings || [],
  },

  setAdminLodgingStatus: {
    project: (lodging) => {
      actions.setAdminLodgingStatus(lodging.id, lodging.status)
      return lodging
    },
    local: (id, status) => {
      actions.setAdminLodgingStatus(id, status)
      return { id, status, enabled: status === "ACTIVE" }
    },
  },
}

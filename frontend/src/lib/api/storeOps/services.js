// Servicios adicionales: proyección + fallback local.
import { actions, store } from "../../state/store"

export const servicesOps = {
  getServices: {
    project: (response) => {
      if (Array.isArray(response)) actions.setServices(response)
      return store.get().services
    },
    local: () => store.get().services || [],
  },

  addService: {
    project: (created) => {
      actions.setServices([...store.get().services, created])
      return created
    },
    local: (data) => actions.addService(data),
  },

  updateService: {
    project: (updated, id, patch) => {
      actions.updateService(id, updated || patch)
      return store.get().services.find((sv) => String(sv.id) === String(id))
    },
    local: (id, patch) => {
      actions.updateService(id, patch)
      return store.get().services.find((sv) => String(sv.id) === String(id))
    },
  },

  removeService: {
    project: (_response, id) => actions.removeService(id),
    local: (id) => actions.removeService(id),
  },

  toggleService: {
    project: (_response, id) => actions.toggleService(id),
    local: (id) => actions.toggleService(id),
  },
}

// Disponibilidad (días bloqueados): proyección + fallback local.
import { actions, store } from "../../state/store"

export const availabilityOps = {
  getBlockedDays: {
    // El backend devuelve un array de fechas ISO ("YYYY-MM-DD") o { dates: [] }.
    project: (response) => {
      const days = Array.isArray(response) ? response : response?.dates || []
      actions.setBlockedDays(days)
      return days
    },
    local: () => store.get().blockedDays || [],
  },

  setBlockedDays: {
    project: (_response, dates) => {
      actions.setBlockedDays(dates)
      return dates
    },
    local: (dates) => {
      actions.setBlockedDays(dates)
      return dates
    },
  },

  toggleBlockedDay: {
    // El backend necesita saber si el día queda bloqueado: sale del store.
    send: (date) => [date, !(store.get().blockedDays || []).includes(date)],
    project: (response, date) => {
      const days = Array.isArray(response) ? response : response?.dates
      if (Array.isArray(days)) actions.setBlockedDays(days)
      else actions.toggleBlockedDay(date)
      return store.get().blockedDays
    },
    local: (date) => {
      actions.toggleBlockedDay(date)
      return store.get().blockedDays
    },
  },
}

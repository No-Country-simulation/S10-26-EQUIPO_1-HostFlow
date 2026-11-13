// Disponibilidad (días bloqueados por el anfitrión).
import { ENDPOINTS as E } from "../endpoints"
import { ApiNotConfiguredError } from "../errors"
import { request } from "../httpClient"

const readBlockedDays = async () => {
  const history = await request(E.getBlockedDays, { name: "getBlockedDays" })
  const dates = new Set()
  if (Array.isArray(history)) {
    history.forEach((item) => {
      if (!item.disabledFrom || item.isActive !== true) return
      const from = new Date(`${String(item.disabledFrom).slice(0, 10)}T00:00:00Z`)
      const until = new Date(`${String(item.disabledUntil || item.disabledFrom).slice(0, 10)}T00:00:00Z`)
      for (let day = from; day <= until; day.setUTCDate(day.getUTCDate() + 1)) {
        dates.add(day.toISOString().slice(0, 10))
      }
    })
  }
  return [...dates]
}

export const availabilityApi = {
  getBlockedDays: readBlockedDays,

  // El backend administra un único período de desactivación activo, no una
  // colección arbitraria de días. La pantalla usa toggleBlockedDay para este
  // contrato; setBlockedDays queda disponible solo para implementaciones demo.
  setBlockedDays: () => {
    throw new ApiNotConfiguredError("setBlockedDays")
  },

  // Alterna un día suelto.
  toggleBlockedDay: async (date, blocked) => {
    if (blocked) {
      await request(E.disableAccommodation, {
        name: "disableAccommodation",
        body: {
          reason: "Bloqueado desde calendario",
          disabledFrom: `${date}T00:00:00`,
          disabledUntil: `${date}T23:59:59`,
        },
      })
      return readBlockedDays()
    }
    await request(E.enableAccommodation, { name: "enableAccommodation" })
    return readBlockedDays()
  },

  getAccommodationStatus: () => request(E.getAccommodationStatus, { name: "getAccommodationStatus" }),

  checkAvailability: (date) =>
    request(E.checkAvailability, {
      name: "checkAvailability",
      query: { date: `${date}T12:00:00` },
    }),
}

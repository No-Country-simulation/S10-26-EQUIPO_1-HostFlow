// Disponibilidad (días bloqueados por el anfitrión).
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const availabilityApi = {
  getBlockedDays: () => request(E.getBlockedDays, { name: "getBlockedDays" }),

  // Reemplaza la lista completa (array de fechas ISO "YYYY-MM-DD").
  setBlockedDays: (dates) => request(E.setBlockedDays, { name: "setBlockedDays", body: { dates } }),

  // Alterna un día suelto. `blocked` lo aporta la fachada (sale del store) y
  // el backend responde con la lista completa resultante.
  toggleBlockedDay: (date, blocked) =>
    request(pathFor(E.toggleBlockedDay, { date }), {
      name: "toggleBlockedDay",
      body: { date, blocked },
    }),
}

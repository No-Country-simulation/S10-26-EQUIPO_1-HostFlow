// Métricas del panel de administración.
//
// Antes estaba implementado en tres lugares (demoApi, httpApi y el fallback de
// la fachada). Acá vive la única versión para que no diverjan: recibe el
// listado de alojamientos y, opcionalmente, las reservas a contar.
export function buildAdminStats({ lodgings, bookings } = {}) {
  const list = Array.isArray(lodgings) ? lodgings : [];
  return {
    lodgings: list.length,
    active: list.filter((l) => l.status === "ACTIVE").length,
    paused: list.filter((l) => l.status === "PAUSED").length,
    hosts: new Set(list.map((l) => l.ownerEmail)).size,
    bookings: Array.isArray(bookings) ? bookings.length : 0,
  };
}

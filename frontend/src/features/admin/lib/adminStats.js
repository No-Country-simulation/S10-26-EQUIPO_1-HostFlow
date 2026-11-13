// Métricas: primero las del backend (/admin/stats); si aún no están, se
// derivan del listado cargado. NUNCA se inventa un número.
export function deriveAdminStats(stats, lodgings) {
  const active = stats?.active ?? lodgings.filter((l) => l.status === "ACTIVE" || l.enabled === true).length;
  const paused = stats?.paused ?? lodgings.filter((l) => l.status === "PAUSED" || l.enabled === false).length;
  const totalBookings = stats?.bookings ?? null;
  const uniqueHosts = stats?.hosts ?? new Set(lodgings.map((l) => l.ownerEmail)).size;
  const lodgingsCount = stats?.lodgings ?? lodgings.length;

  return { lodgingsCount, active, paused, totalBookings, uniqueHosts };
}

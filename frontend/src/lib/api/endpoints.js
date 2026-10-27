// CONTRATO DE API — HostFlow (backend Java Spring Boot).
//
// Fuente de verdad: el propio backend (Swagger UI en /api/v1/swagger-ui.html,
// especificación OpenAPI en /api/v1/api-docs) y docs/swagger-api.md.
//
// `VITE_API_URL` debe apuntar a la BASE del contexto, es decir incluir el
// context-path configurado en el backend (`server.servlet.context-path=/api/v1`).
//   .env  →  VITE_API_URL=http://localhost:8080/api/v1
//
// A partir de ahí las rutas son relativas a esa base.
export const ENDPOINTS = {
  // ── Auth (públicos) ───────────────────────────────────────────────────
  login: { method: "POST", path: "/auth/login" },
  register: { method: "POST", path: "/auth/register" },
  // Solicita el envío del código de recuperación de 6 dígitos.
  forgotPassword: { method: "POST", path: "/auth/forgot-password" },
  // Restablece la contraseña con { email, code, newPassword }.
  resetPassword: { method: "POST", path: "/auth/reset-password" },

  // ── Dueños de alojamiento (requieren JWT) ──────────────────────────────
  getOwnerByEmail: { method: "GET", path: "/owners/email/{email}" },
  getOwnerById: { method: "GET", path: "/owners/{id}" },
  // Solo ROLE_ADMIN
  getAllOwners: { method: "GET", path: "/owners" },
  // Solo ROLE_ADMIN → ?enabled=true|false
  toggleOwnerStatus: { method: "PATCH", path: "/owners/{id}/status" },

  // ── Reservado: módulos PENDIENTES en el backend ─────────────────────────
  // El backend todavía no expone estas rutas (ver docs/swagger-api.md). Mientras
  // tanto, src/lib/api/index.js cae al store local y las pantallas siguen
  // siendo usables. Cuando el backend las implemente, alcanza con quitar el
  // comentario de la entrada correspondiente: las pantallas no cambian.
  // Alojamiento:        "/lodgings/mine"
  // Disponibilidad:     "/lodgings/mine/blocked-days"
  // Reservas:           "/lodgings/mine/bookings", "/bookings/{id}"
  // Estado del pago:    "/bookings/{id}/payment-status"
  // Datos bancarios:    "/lodgings/mine/bank-details"
  // Servicios:          "/lodgings/mine/services"
  // Onboarding huésped: "/onboarding/{lodgingId}/{bookingId}" (público)
  // Cotización lancha:  "/bookings/{id}/boat-quote"
}

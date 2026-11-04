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
//
// Estado de implementación en el backend:
//   · Auth y Owners  → IMPLEMENTADOS (AuthController / OwnerController).
//   · El resto       → CONTRATO RESERVADO. Mientras el backend no los exponga,
//                      httpApi.js detecta el 404/405/501 y cae al store local
//                      para que la pantalla siga siendo usable sin romperse.
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

  // ── Alojamiento (datos del alojamiento del dueño autenticado) ──────────
  getLodging: { method: "GET", path: "/lodgings/mine" },
  updateLodging: { method: "PUT", path: "/lodgings/mine" },
  // Marca del alojamiento: 3 colores + forma del borde + patrón de fondo.
  updateBrand: { method: "PUT", path: "/lodgings/mine/brand" },
  // Logo y fotos: el backend guarda la referencia; el front sube dataURL.
  setLogo: { method: "PUT", path: "/lodgings/mine/logo" },
  addImage: { method: "POST", path: "/lodgings/mine/images" },
  removeImage: { method: "DELETE", path: "/lodgings/mine/images/{index}" },

  // ── Disponibilidad (días bloqueados por el anfitrión) ──────────────────
  getBlockedDays: { method: "GET", path: "/lodgings/mine/blocked-days" },
  // Reemplaza la lista completa (array de fechas ISO "YYYY-MM-DD").
  setBlockedDays: { method: "PUT", path: "/lodgings/mine/blocked-days" },
  // Alterna un día suelto; body: { date, blocked }.
  toggleBlockedDay: { method: "PATCH", path: "/lodgings/mine/blocked-days/{date}" },

  // ── Reservas ──────────────────────────────────────────────────────────
  getBookings: { method: "GET", path: "/lodgings/mine/bookings" },
  createBooking: { method: "POST", path: "/lodgings/mine/bookings" },
  updateBooking: { method: "PATCH", path: "/bookings/{id}" },
  setBookingStatus: { method: "PATCH", path: "/bookings/{id}/status" },
  // Estado del pago de una reserva: { paymentStatus: "PENDING" | "PAID" }.
  setPaymentStatus: { method: "PATCH", path: "/bookings/{id}/payment-status" },
  // Renueva el enlace de llegada del huésped (nueva vigencia de 24 h).
  regenerateOnboardingLink: { method: "POST", path: "/bookings/{id}/onboarding-link" },

  // ── Onboarding del huésped (B2C, público: enlace único) ────────────────
  // Lectura pública del enlace: devuelve el alojamiento, la reserva, los
  // servicios habilitados y las formas de pago activas del huésped.
  getOnboarding: { method: "GET", path: "/onboarding/{lodgingId}/{bookingId}" },
  // Ficha completada por el huésped (datos, alérgenos, servicios elegidos).
  submitOnboarding: { method: "POST", path: "/onboarding/{lodgingId}/{bookingId}" },
  // Cotización de transporte en lancha (ver docs/boat-quote-contract.md).
  createBoatQuoteRequest: { method: "POST", path: "/bookings/{id}/boat-quote" },
  declineBoatQuote: { method: "DELETE", path: "/bookings/{id}/boat-quote" },

  // ── Servicios adicionales ─────────────────────────────────────────────
  getServices: { method: "GET", path: "/lodgings/mine/services" },
  addService: { method: "POST", path: "/lodgings/mine/services" },
  updateService: { method: "PATCH", path: "/lodgings/mine/services/{id}" },
  removeService: { method: "DELETE", path: "/lodgings/mine/services/{id}" },
  toggleService: { method: "PATCH", path: "/lodgings/mine/services/{id}/status" },

  // ── Métodos de pago (máximo MAX_PAYMENT_METHODS por alojamiento) ───────
  // Devuelve el agregado: { primary, wallets, methods, bankDetails }.
  getPayments: { method: "GET", path: "/lodgings/mine/payment-methods" },
  addPaymentMethod: { method: "POST", path: "/lodgings/mine/payment-methods" },
  updatePaymentMethod: { method: "PATCH", path: "/lodgings/mine/payment-methods/{id}" },
  removePaymentMethod: { method: "DELETE", path: "/lodgings/mine/payment-methods/{id}" },
  togglePaymentMethod: { method: "PATCH", path: "/lodgings/mine/payment-methods/{id}/status" },
  // Moneda principal y billeteras secundarias.
  setPrimaryCurrency: { method: "PUT", path: "/lodgings/mine/payment-methods/currency" },
  toggleWallet: { method: "PUT", path: "/lodgings/mine/payment-methods/wallets" },
  // Datos bancarios que el huésped usa para transferir (cobro externo).
  updateBankDetails: { method: "PUT", path: "/lodgings/mine/bank-details" },

  // ── Módulo de administración (solo ROLE_ADMIN) ────────────────────────
  // Métricas agregadas: { lodgings, active, paused, hosts, bookings }.
  getAdminStats: { method: "GET", path: "/admin/stats" },
  // Listado de alojamientos: el backend hoy lo expone como listado de dueños.
  getAdminLodgings: { method: "GET", path: "/owners" },
  // Pausa / reactiva un alojamiento habilitando o deshabilitando su dueño.
  setAdminLodgingStatus: { method: "PATCH", path: "/owners/{id}/status" },
}

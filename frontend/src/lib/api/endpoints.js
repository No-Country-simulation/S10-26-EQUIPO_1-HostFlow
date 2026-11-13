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
  forgotPassword: { method: "POST", path: "/auth/forgot-password" },
  resetPassword: { method: "POST", path: "/auth/reset-password" },
  verifyEmail: { method: "GET", path: "/auth/verify-email" },
  resendVerification: { method: "POST", path: "/auth/resend-verification" },

  // ── Dueños de alojamiento (requieren JWT) ──────────────────────────────
  getOwnerByEmail: { method: "GET", path: "/owners/email/{email}" },
  getOwnerById: { method: "GET", path: "/owners/{id}" },
  getAllOwners: { method: "GET", path: "/owners" },
  toggleOwnerStatus: { method: "PATCH", path: "/owners/{id}/status" },

  // ── Alojamiento (Perfil y Galería) ─────────────────────────────────────
  getLodging: { method: "GET", path: "/owners/accommodation/profile" },
  createLodging: { method: "POST", path: "/owners/accommodation/profile" },
  updateLodging: { method: "PATCH", path: "/owners/accommodation/profile" },
  updateBrand: { method: "PATCH", path: "/owners/accommodation/profile" },
  setLogo: { method: "POST", path: "/owners/accommodation/profile/logo" },
  getGallery: { method: "GET", path: "/owners/accommodation/gallery" },
  addImage: { method: "POST", path: "/owners/accommodation/gallery/upload" },
  removeImage: { method: "DELETE", path: "/owners/accommodation/gallery/{id}" },

  // ── Disponibilidad (Desactivaciones temporales) ────────────────────────
  getAccommodationStatus: { method: "GET", path: "/owners/accommodation/status" },
  getBlockedDays: { method: "GET", path: "/owners/accommodation/history" },
  disableAccommodation: { method: "POST", path: "/owners/accommodation/disable" },
  enableAccommodation: { method: "POST", path: "/owners/accommodation/enable" },
  checkAvailability: { method: "GET", path: "/owners/accommodation/availability" },

  // ── Reservas ──────────────────────────────────────────────────────────
  getBookings: { method: "GET", path: "/owners/reservations" },
  getBookingById: { method: "GET", path: "/owners/reservations/{id}" },
  createBooking: { method: "POST", path: "/owners/reservations" },
  cancelBooking: { method: "POST", path: "/owners/reservations/{id}/cancel" },
  completeBooking: { method: "POST", path: "/owners/reservations/{id}/complete" },
  markBookingPaid: { method: "POST", path: "/owners/reservations/{id}/mark-paid" },
  checkReservationAvailability: { method: "GET", path: "/owners/reservations/check-availability" },
  regenerateOnboardingLink: { method: "POST", path: "/owners/reservations/{id}/reactivate-link" },

  // ── Onboarding del huésped (B2C, público) ─────────────────────────────
  getOnboarding: { method: "GET", path: "/public/reservations/confirm" },
  submitOnboarding: { method: "POST", path: "/public/reservations/confirm" },

  // ── Servicios adicionales ─────────────────────────────────────────────
  getServices: { method: "GET", path: "/owners/accommodation/additional-services" },
  addService: { method: "POST", path: "/owners/accommodation/additional-services" },
  updateService: { method: "PATCH", path: "/owners/accommodation/additional-services/{id}" },
  removeService: { method: "DELETE", path: "/owners/accommodation/additional-services/{id}" },
  toggleService: { method: "PATCH", path: "/owners/accommodation/additional-services/{id}" },
  getServiceImages: { method: "GET", path: "/owners/accommodation/additional-services/{serviceId}/images" },
  addServiceImage: { method: "POST", path: "/owners/accommodation/additional-services/{serviceId}/images" },
  removeServiceImage: { method: "DELETE", path: "/owners/accommodation/additional-services/{serviceId}/images/{imageId}" },

  // ── Métodos de pago y monedas ─────────────────────────────────────────
  getPayments: { method: "GET", path: "/owners/accommodation/payment-methods" },
  getCurrencies: { method: "GET", path: "/owners/accommodation/currencies" },
  upsertCurrency: { method: "PUT", path: "/owners/accommodation/currencies" },
  addPaymentMethod: { method: "POST", path: "/owners/accommodation/payment-methods" },
  updatePaymentMethod: { method: "PATCH", path: "/owners/accommodation/payment-methods/{id}" },
  removePaymentMethod: { method: "DELETE", path: "/owners/accommodation/payment-methods/{id}" },
  togglePaymentMethod: { method: "PATCH", path: "/owners/accommodation/payment-methods/{id}" },
  toggleWallet: { method: "PUT", path: "/owners/accommodation/currencies" },

  // ── Módulo de administración (solo ROLE_ADMIN) y Métricas ─────────────
  getOwnerMetrics: { method: "GET", path: "/owners/dashboard/metrics" },
  getAdminStats: { method: "GET", path: "/admin/dashboard/metrics" },
  getAdminLodgings: { method: "GET", path: "/owners" },
  setAdminLodgingStatus: { method: "PATCH", path: "/owners/{id}/status" },

  // Endpoints públicos de huéspedes y precios (catálogo consultable sin sesión).
  createOrGetGuest: { method: "POST", path: "/public/guests" },
  getGuestByEmail: { method: "GET", path: "/public/guests/email/{email}" },
  getPublicBasePrice: { method: "GET", path: "/public/accommodations/{accommodationId}/base-price" },
  getPublicServices: { method: "GET", path: "/public/accommodations/{accommodationId}/additional-services" },
}

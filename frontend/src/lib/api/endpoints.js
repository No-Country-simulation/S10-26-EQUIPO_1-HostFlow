// CONTRATO DE API — HostFlow (backend Java Spring Boot).
//
// Fuente de verdad: backend controllers y OpenAPI en /api/v1/api-docs.
// `VITE_API_URL` debe apuntar a la BASE del contexto (`http://localhost:8080/api/v1`).
// A partir de ahí las rutas son relativas a esa base.

export const ENDPOINTS = {
  // ── Auth (públicos) ───────────────────────────────────────────────────
  login: { method: "POST", path: "/auth/login" },
  register: { method: "POST", path: "/auth/register" },
  forgotPassword: { method: "POST", path: "/auth/forgot-password" },
  resetPassword: { method: "POST", path: "/auth/reset-password" },
  verifyEmail: { method: "GET", path: "/auth/verify-email" },
  resendVerification: { method: "POST", path: "/auth/resend-verification" },

  // ── Perfil y galería de alojamiento (ROLE_OWNER) ──────────────────────
  getAccommodationProfile: { method: "GET", path: "/owners/accommodation/profile" },
  createAccommodationProfile: { method: "POST", path: "/owners/accommodation/profile" },
  updateAccommodationProfile: { method: "PATCH", path: "/owners/accommodation/profile" },
  getAccommodationGallery: { method: "GET", path: "/owners/accommodation/gallery" },
  uploadAccommodationGalleryImage: { method: "POST", path: "/owners/accommodation/gallery/upload" },
  uploadAccommodationLogo: { method: "POST", path: "/owners/accommodation/profile/logo" },
  deleteAccommodationGalleryImage: { method: "DELETE", path: "/owners/accommodation/gallery/{id}" },

  // ── Disponibilidad y desactivaciones (ROLE_OWNER) ─────────────────────
  getAccommodationStatus: { method: "GET", path: "/owners/accommodation/status" },
  getAccommodationDisableHistory: { method: "GET", path: "/owners/accommodation/history" },
  disableAccommodation: { method: "POST", path: "/owners/accommodation/disable" },
  enableAccommodation: { method: "POST", path: "/owners/accommodation/enable" },
  checkAvailability: { method: "GET", path: "/owners/accommodation/availability" },

  // ── Monedas (ROLE_OWNER) ──────────────────────────────────────────────
  getAccommodationCurrencies: { method: "GET", path: "/owners/accommodation/currencies" },
  upsertAccommodationCurrency: { method: "PUT", path: "/owners/accommodation/currencies" },

  // ── Métodos de pago (ROLE_OWNER) ──────────────────────────────────────
  getAccommodationPaymentMethods: { method: "GET", path: "/owners/accommodation/payment-methods" },
  createAccommodationPaymentMethod: { method: "POST", path: "/owners/accommodation/payment-methods" },
  updateAccommodationPaymentMethod: { method: "PATCH", path: "/owners/accommodation/payment-methods/{id}" },
  deleteAccommodationPaymentMethod: { method: "DELETE", path: "/owners/accommodation/payment-methods/{id}" },

  // ── Reservas (ROLE_OWNER) ─────────────────────────────────────────────
  getReservations: { method: "GET", path: "/owners/reservations" },
  getReservationById: { method: "GET", path: "/owners/reservations/{id}" },
  createReservation: { method: "POST", path: "/owners/reservations" },
  cancelReservation: { method: "POST", path: "/owners/reservations/{id}/cancel" },
  completeReservation: { method: "POST", path: "/owners/reservations/{id}/complete" },
  reactivateReservationLink: { method: "POST", path: "/owners/reservations/{id}/reactivate-link" },

  // ── Dashboard Metrics (ROLE_OWNER / ROLE_ADMIN) ───────────────────────
  getOwnerMetrics: { method: "GET", path: "/owners/dashboard/metrics" },
  getAdminMetrics: { method: "GET", path: "/admin/dashboard/metrics" },

  // ── Dueños de alojamiento (Solo ROLE_ADMIN) ───────────────────────────
  getAllOwners: { method: "GET", path: "/owners" },
  getOwnerById: { method: "GET", path: "/owners/{id}" },
  getOwnerByEmail: { method: "GET", path: "/owners/email/{email}" },
  toggleOwnerStatus: { method: "PATCH", path: "/owners/{id}/status" },
}

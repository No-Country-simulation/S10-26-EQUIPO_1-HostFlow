// Punto único de acceso a datos para la interfaz.
// Las pantallas importan `api` (o `actions` desde el contexto) y nunca hablan con fetch
// ni con datos de prueba directamente.
//   Sin VITE_API_URL → demoApi (datos en memoria, src/services/demoApi.js).
//   Con    VITE_API_URL → httpApi (backend real Java Spring Boot, src/services/httpApi.js).
import { DEMO_MODE } from "../config/env"
import { demoApi } from "./demoApi"
import { httpApi } from "./httpApi"
import { actions, store } from "../data/store"
import { tokenStorage } from "./tokenStorage"

const impl = DEMO_MODE ? demoApi : httpApi

// El token lo emite el backend; aquí solo se conserva para enviarlo después.
// El usuario también se guarda en el store: es lo que protege las rutas privadas
// de la interfaz (en modo demo el login ya lo deja seteado).
const keepToken = (result) => {
  if (result?.token) tokenStorage.set(result.token)
  if (result?.user) actions.setUser(result.user)
  return result
}

// Operation helper.
// El backend todavía no expone los módulos de alojamiento, reservas, disponibilidad
// ni pagos. Para esos casos se cae al store local: las pantallas siguen siendo
// usables y, sobre todo, NO fallan en silencio (que era el bug anterior).
const op =
  (name) =>
  (...args) => {
    const fn = impl[name] || demoApi[name]
    if (typeof fn !== "function") {
      throw new Error(`La operación "${name}" no está implementada en ningún adaptador.`)
    }
    return fn(...args)
  }

export const api = {
  // ── Auth ────────────────────────────────────────────────────────────────
  login: async (credentials) => keepToken(await impl.login(credentials)),
  // Acceso rápido de la demo: solo existe en modo demo.
  demoLogin: async (email) => {
    if (typeof impl.demoLogin !== "function") {
      throw new Error("El acceso rápido solo está disponible en modo demostración.")
    }
    return keepToken(await impl.demoLogin(email))
  },
  register: async (data) => keepToken(await impl.register(data)),
  // Equivale a POST /auth/forgot-password
  forgotPassword: (email) => impl.forgotPassword(email),
  // Equivale a POST /auth/reset-password.
  // La interfaz usa `password`; el backend espera `newPassword`: se envían ambos.
  resetPassword: ({ email, code, password }) =>
    impl.resetPassword({ email, code, password, newPassword: password }),
  logout: () => {
    // Siempre hay que limpiar el store: con backend real no hay endpoint de logout,
    // pero el usuario en memoria mantiene abierta la sesión de la interfaz.
    actions.setUser(null)
    tokenStorage.clear()
  },

  // ── Alojamiento ─────────────────────────────────────────────────────────
  getLodging: op("getLodging"),
  updateLodging: op("updateLodging"),
  setLogo: op("setLogo"),
  addImage: op("addImage"),
  removeImage: op("removeImage"),
  updateBrand: op("updateBrand"),

  // ── Disponibilidad ──────────────────────────────────────────────────────
  getBlockedDays: op("getBlockedDays"),
  setBlockedDays: op("setBlockedDays"),
  toggleBlockedDay: op("toggleBlockedDay"),

  // ── Reservas ────────────────────────────────────────────────────────────
  getBookings: op("getBookings"),
  updateBooking: op("updateBooking"),
  setBookingStatus: op("setBookingStatus"),

  // ── Pagos ───────────────────────────────────────────────────────────────
  getPayments: op("getPayments"),
  setPrimaryCurrency: op("setPrimaryCurrency"),
  toggleWallet: op("toggleWallet"),
  togglePaymentMethod: op("togglePaymentMethod"),
  addPaymentMethod: op("addPaymentMethod"),

  // ── Admin ───────────────────────────────────────────────────────────────
  getAdminLodgings: op("getAdminLodgings"),
  setAdminLodgingStatus: op("setAdminLodgingStatus"),

  // En demo siempre hay acceso si existe un usuario en el store.
  isAuthenticated: () => (DEMO_MODE ? Boolean(store.get().user) : Boolean(tokenStorage.get())),
}

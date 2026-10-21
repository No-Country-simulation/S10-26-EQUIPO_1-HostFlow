// Punto único de acceso a datos para la interfaz.
// Las pantallas importan `api` (o `actions` desde el contexto) y nunca hablan con fetch
// ni con datos de prueba directamente.
//   Sin VITE_API_URL → demoApi (datos en memoria, src/services/demoApi.js).
//   Con    VITE_API_URL → httpApi (backend real Java Spring Boot, src/services/httpApi.js).
import { DEMO_MODE } from "../config/env"
import { demoApi } from "./demoApi"
import { httpApi } from "./httpApi"
import { store } from "../data/store"
import { tokenStorage } from "./tokenStorage"

const impl = DEMO_MODE ? demoApi : httpApi

// El token lo emite el backend; aquí solo se conserva para enviarlo después.
const keepToken = (result) => {
  if (result?.token) tokenStorage.set(result.token)
  return result
}

export const api = {
  // ── Auth ────────────────────────────────────────────────────────────────
  login: async (credentials) => keepToken(await impl.login(credentials)),
  demoLogin: async (email) => keepToken(await impl.demoLogin(email)),
  register: async (data) => keepToken(await impl.register(data)),
  // Equivale a POST /auth/forgot-password
  forgotPassword: (email) => impl.forgotPassword(email),
  // Equivale a POST /auth/reset-password.
  // La interfaz usa `password`; el backend espera `newPassword`: se envían ambos.
  resetPassword: ({ email, code, password }) =>
    impl.resetPassword({ email, code, password, newPassword: password }),
  logout: () => {
    if (typeof impl.logout === "function") impl.logout()
    tokenStorage.clear()
  },

  // ── Operaciones de datos (mismas en demo y backend) ─────────────────────
  getLodging: (...args) => impl.getLodging?.(...args),
  updateLodging: (...args) => impl.updateLodging?.(...args),
  getBlockedDays: (...args) => impl.getBlockedDays?.(...args),
  setBlockedDays: (...args) => impl.setBlockedDays?.(...args),
  getBookings: (...args) => impl.getBookings?.(...args),
  updateBooking: (...args) => impl.updateBooking?.(...args),
  setBookingStatus: (...args) => impl.setBookingStatus?.(...args),
  getPayments: (...args) => impl.getPayments?.(...args),
  setPrimaryCurrency: (...args) => impl.setPrimaryCurrency?.(...args),
  toggleWallet: (...args) => impl.toggleWallet?.(...args),
  togglePaymentMethod: (...args) => impl.togglePaymentMethod?.(...args),
  addPaymentMethod: (...args) => impl.addPaymentMethod?.(...args),
  setLogo: (...args) => impl.setLogo?.(...args),
  addImage: (...args) => impl.addImage?.(...args),
  removeImage: (...args) => impl.removeImage?.(...args),
  updateBrand: (...args) => impl.updateBrand?.(...args),
  getAdminLodgings: (...args) => impl.getAdminLodgings?.(...args),
  setAdminLodgingStatus: (...args) => impl.setAdminLodgingStatus?.(...args),

  // En demo siempre hay acceso si existe un usuario en el store.
  isAuthenticated: () => (DEMO_MODE ? Boolean(store.get().user) : Boolean(tokenStorage.get())),
}

// Punto único de acceso a datos para la interfaz.
// Las pantallas importan `api` (o `actions` desde el contexto) y nunca hablan con fetch
// ni con datos de prueba directamente.
//   Sin VITE_API_URL → demoApi (datos en memoria, src/lib/api/demoApi.js).
//   Con    VITE_API_URL → httpApi (backend real Java Spring Boot, src/lib/api/httpApi.js).
//
// ESTA fachada es el ÚNICO punto que resuelve el fallback: el adaptador remoto
// solo ejecuta HTTP y nunca toca el store. Cuando el endpoint todavía no
// existe (404 / 405 / 501, o falta de conexión) la operación se resuelve
// contra el store local (storeOps.js) y, en último recurso, contra las
// acciones demo. Así ninguna pantalla se rompe mientras el backend publica los
// módulos pendientes.
import { DEMO_MODE } from "../config/env"
import { actions, store } from "../state/store"
import { buildAdminStats } from "./adminStats"
import { demoApi } from "./demoApi"
import { ApiNotConfiguredError, isEndpointUnavailable } from "./errors"
import { httpApi } from "./httpApi"
import { tokenStorage } from "./tokenStorage"
import { storeOps } from "./storeOps"

const impl = DEMO_MODE ? demoApi : httpApi

// El token lo emite el backend; aquí solo se conserva para enviarlo después.
// El usuario también se guarda en el store: es lo que protege las rutas privadas
// de la interfaz (en modo demo el login ya lo deja seteado).
const keepToken = (result) => {
  if (result?.token) tokenStorage.set(result.token)
  if (result?.user) actions.setUser(result.user)
  return result
}

// Último recurso para una operación sin adaptador ni implementación: se
// resuelve con las acciones del store o leyendo su estado directamente.
const storeRead = (name, args) => {
  if (typeof actions[name] === "function") return actions[name](...args)
  const s = store.get()
  if (name === "getLodging") return s.lodging
  if (name === "getAdminLodgings") return s.admin?.lodgings || []
  if (name === "getAdminStats") return buildAdminStats({ lodgings: s.admin?.lodgings })
  if (name === "getBlockedDays") return s.blockedDays || []
  if (name === "getBookings") return s.bookings || []
  if (name === "getPayments") return s.payments || {}
  if (name === "getServices") return s.services || []
  return undefined
}

// Operation helper.
// El backend todavía no expone todos los módulos. Si la operación no está
// declarada en endpoints.js (`ApiNotConfiguredError`), o el endpoint responde
// 404 / 405 / 501 (o no hay conexión), se cae con seguridad al store local
// sin romper la aplicación. Cualquier otro error es REAL y se propaga: así la
// pantalla puede mostrarlo al usuario.
const op =
  (name) =>
  async (...args) => {
    // 1) Backend real: HTTP puro + proyección de la respuesta en el store.
    if (!DEMO_MODE && typeof httpApi[name] === "function") {
      try {
        const ops = storeOps[name]
        const send = typeof ops?.send === "function" ? ops.send(...args) : args
        const result = await httpApi[name](...send)
        return typeof ops?.project === "function" ? ops.project(result, ...args) : result
      } catch (error) {
        if (isEndpointUnavailable(error)) {
          const local = storeOps[name]?.local
          if (typeof local === "function") return local(...args)
          throw error
        }
        if (!(error instanceof ApiNotConfiguredError)) throw error
        console.warn(`Operación "${name}":`, error)
      }
    }

    // 2) Modo demo, u operación que httpApi todavía no expone.
    if (DEMO_MODE || typeof httpApi[name] !== "function") {
      const fn = demoApi[name]
      if (typeof fn === "function") return fn(...args)
    }

    // 3) Sin implementación: acciones del store o lectura directa.
    return storeRead(name, args)
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
  createBooking: op("createBooking"),
  updateBooking: op("updateBooking"),
  setBookingStatus: op("setBookingStatus"),
  // Estado del pago de una reserva (Pendiente / Pagado).
  setPaymentStatus: op("setPaymentStatus"),
  // Renueva el enlace de llegada del huésped: nueva vigencia de 24 h.
  regenerateOnboardingLink: op("regenerateOnboardingLink"),

  // ── Onboarding del huésped (B2C) ────────────────────────────────────────
  // Lectura pública del enlace de llegada (sin token).
  getOnboarding: op("getOnboarding"),
  submitOnboarding: op("submitOnboarding"),
  // Solicitud de cotización de lancha: genera el JSON; NO lo envía a otra API.
  createBoatQuoteRequest: op("createBoatQuoteRequest"),
  declineBoatQuote: op("declineBoatQuote"),

  // ── Servicios adicionales ───────────────────────────────────────────────
  getServices: op("getServices"),
  addService: op("addService"),
  updateService: op("updateService"),
  removeService: op("removeService"),
  toggleService: op("toggleService"),

  // ── Pagos ───────────────────────────────────────────────────────────────
  getPayments: op("getPayments"),
  setPrimaryCurrency: op("setPrimaryCurrency"),
  toggleWallet: op("toggleWallet"),
  togglePaymentMethod: op("togglePaymentMethod"),
  addPaymentMethod: op("addPaymentMethod"),
  updatePaymentMethod: op("updatePaymentMethod"),
  removePaymentMethod: op("removePaymentMethod"),
  // Datos bancarios del alojamiento (transferencia externa).
  updateBankDetails: op("updateBankDetails"),

  // ── Admin ───────────────────────────────────────────────────────────────
  getAdminLodgings: op("getAdminLodgings"),
  getAdminStats: op("getAdminStats"),
  setAdminLodgingStatus: op("setAdminLodgingStatus"),

  // En demo siempre hay acceso si existe un usuario en el store.
  isAuthenticated: () => (DEMO_MODE ? Boolean(store.get().user) : Boolean(tokenStorage.get())),
}

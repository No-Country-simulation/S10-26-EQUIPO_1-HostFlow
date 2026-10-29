// Adaptador REAL hacia el backend Java Spring Boot (se usa cuando VITE_API_URL está definida).
//
// Cada función corresponde a una entrada de endpoints.js. Los cuerpos y las
// respuestas están alineados con los DTOs del backend:
//   · LoginRequest          { email, password }
//   · OwnerRegisterRequest  { fullName, accommodationName, email, password, termsAccepted }
//   · ForgotPasswordRequest { email }
//   · ResetPasswordRequest  { email, code, newPassword }
//   · AuthResponse          { token, tokenType, userId, email, fullName, roles }
//
// La conversión al formato interno de la interfaz (`{ token, user }`) se hace
// AQUÍ, nunca en las pantallas.
//
// ── Módulos todavía no implementados en el backend ─────────────────────────
// Auth y Owners existen; el resto (alojamiento, disponibilidad, reservas,
// servicios, pagos y estadísticas de administración) está RESERVADO en el
// contrato. Mientras tanto `withLocalFallback` detecta que el endpoint aún no
// responde (404 / 405 / 501, o falta de conexión) y opera sobre el store local
// de modo que NINGUNA pantalla se rompa. Cuando el backend los publique, la
// app pasa a usarlos sin tocar las pantallas.
//
// Toda lectura exitosa se escribe en el store: las pantallas se suscriben con
// `useStore`, no guardan el resultado de las llamadas.
import { ENDPOINTS as E } from "./endpoints"
import { request } from "./httpClient"
import { actions, store } from "../state/store"
import { MAX_IMAGES, MAX_PAYMENT_METHODS } from "../state/store"

// Rellena los placeholders de la ruta: pathFor(E.toggleBlockedDay, { date })
//   → "/lodgings/mine/blocked-days/2026-10-30"
const pathFor = (endpoint, params = {}) => ({
  ...endpoint,
  path: Object.entries(params).reduce(
    (path, [key, value]) => path.replace(`{${key}}`, encodeURIComponent(String(value))),
    endpoint.path,
  ),
})

// Un 404 / 405 / 501 (o sin conexión) significa "este endpoint todavía no
// existe en el backend": se cae al store local. El resto de los errores (400,
// 403, 409, 500…) son reales y se propagan a la pantalla.
const isEndpointUnavailable = (error) => {
  const status = error?.status
  return status === 0 || status === 404 || status === 405 || status === 501
}

async function withLocalFallback(remote, local) {
  try {
    return await remote()
  } catch (error) {
    if (!isEndpointUnavailable(error)) throw error
    return local()
  }
}

// AuthResponse → usuario interno de la app.
const toUser = (response) => {
  const roles = response?.roles || []
  return {
    id: response?.userId ?? null,
    name: response?.fullName ?? "",
    email: response?.email ?? "",
    roles,
    role: roles.some((role) => /ADMIN/i.test(role)) ? "ADMIN" : "HOST",
  }
}

const toAuth = (response) => ({
  token: response?.token ?? null,
  tokenType: response?.tokenType ?? "Bearer",
  user: toUser(response),
})

// Backend → estado interno del alojamiento. Se acepta el shape documentado y
// se conserva el valor local cuando el backend no manda el campo.
function mergeLodging(dto) {
  const current = store.get().lodging
  if (!dto || typeof dto !== "object") return current
  return {
    ...current,
    name: dto.name || dto.accommodationName || current.name,
    tagline: dto.tagline ?? current.tagline,
    location: dto.location ?? current.location,
    phone: dto.phone ?? current.phone,
    rooms: Number(dto.rooms ?? current.rooms),
    bathrooms: Number(dto.bathrooms ?? current.bathrooms),
    capacity: Number(dto.capacity ?? current.capacity),
    pricePerNight: Number(dto.pricePerNight ?? current.pricePerNight),
    currency: dto.currency || current.currency,
    images: Array.isArray(dto.images) ? dto.images.slice(0, MAX_IMAGES) : current.images,
    logo: dto.logo ?? current.logo,
    brand: dto.brand ? { ...current.brand, ...dto.brand } : current.brand,
    brandPalette: Array.isArray(dto.brandPalette) ? dto.brandPalette : current.brandPalette,
  }
}

// Backend → reserva interna: los nombres del DTO pueden cambiar; los campos
// que consume la interfaz quedan normalizados.
function toBooking(dto) {
  const current = store.get().bookings.find((b) => String(b.id) === String(dto?.id))
  return {
    ...current,
    ...dto,
    id: dto.id,
    guestName: dto.guestName || current?.guestName || "",
    guestEmail: dto.guestEmail || current?.guestEmail || "",
    guestPhone: dto.guestPhone || current?.guestPhone || "",
    checkIn: dto.checkIn || current?.checkIn || "",
    checkOut: dto.checkOut || current?.checkOut || "",
    guests: Number(dto.guests ?? current?.guests ?? 1),
    total: Number(dto.total ?? current?.total ?? 0),
    currency: dto.currency || current?.currency || store.get().lodging.currency,
    status: dto.status || current?.status || "PENDING",
    paymentStatus: dto.paymentStatus || current?.paymentStatus || "PENDING",
    additional: dto.additional ?? current?.additional ?? "",
    selectedServices: Array.isArray(dto.selectedServices)
      ? dto.selectedServices
      : current?.selectedServices || [],
    onboardingLinkIssuedAt: dto.onboardingLinkIssuedAt || current?.onboardingLinkIssuedAt,
    onboarding: dto.onboarding || current?.onboarding,
    boatQuote: dto.boatQuote || current?.boatQuote,
  }
}

// OwnerResponse → fila del módulo de administración. El backend de dueños no
// expone la cantidad de habitaciones, así que queda en null y la tabla la
// muestra como "—" en lugar de inventar un número.
const toAdminLodging = (owner) => ({
  id: String(owner.id),
  name: owner.accommodationName || "Alojamiento sin nombre",
  owner: owner.fullName || "Sin nombre",
  ownerEmail: owner.email || "",
  rooms: null,
  status: owner.enabled ? "ACTIVE" : "PAUSED",
  enabled: Boolean(owner.enabled),
  createdAt: owner.createdAt,
  roles: owner.roles || [],
})

export const httpApi = {
  // ── Auth ────────────────────────────────────────────────────────────────
  login: async (credentials) =>
    toAuth(await request(E.login, { name: "login", body: credentials, auth: false })),

  register: async (data) =>
    toAuth(
      await request(E.register, {
        name: "register",
        body: {
          fullName: data.fullName,
          accommodationName: data.accommodationName ?? data.lodgingName,
          email: data.email,
          password: data.password,
          termsAccepted: data.termsAccepted ?? true,
        },
        auth: false,
      }),
    ),

  // Devuelve { message, email, expirationMinutes }.
  forgotPassword: (email) =>
    request(E.forgotPassword, { name: "forgotPassword", body: { email }, auth: false }),

  // El backend espera `code` (6 dígitos) y `newPassword`.
  resetPassword: ({ email, code, newPassword }) =>
    request(E.resetPassword, {
      name: "resetPassword",
      body: { email, code, newPassword },
      auth: false,
    }),

  // ── Dueños de alojamiento (JWT) ─────────────────────────────────────────
  getOwnerByEmail: (email) =>
    request(pathFor(E.getOwnerByEmail, { email }), { name: "getOwnerByEmail" }),

  getOwnerById: (id) => request(pathFor(E.getOwnerById, { id }), { name: "getOwnerById" }),

  getAllOwners: () => request(E.getAllOwners, { name: "getAllOwners" }),

  toggleOwnerStatus: (id, enabled) =>
    request(
      { ...pathFor(E.toggleOwnerStatus, { id }), path: `${pathFor(E.toggleOwnerStatus, { id }).path}?enabled=${enabled}` },
      { name: "toggleOwnerStatus" },
    ),

  // ── Alojamiento (datos del alojamiento del dueño autenticado) ───────────
  // Lee /lodgings/mine. Mientras ese endpoint no exista, completa lo único que
  // el backend sí sabe (nombre del alojamiento y titularidad) desde el dueño.
  getLodging: () =>
    withLocalFallback(
      async () => {
        const lodging = await request(E.getLodging, { name: "getLodging" })
        actions.updateLodging(mergeLodging(lodging))
        return store.get().lodging
      },
      async () => {
        const email = store.get().user?.email
        if (email) {
          try {
            const owner = await request(pathFor(E.getOwnerByEmail, { email }), { name: "getOwnerByEmail" })
            actions.updateLodging(
              mergeLodging({
                name: owner?.accommodationName,
                owner: owner?.fullName,
                ownerEmail: owner?.email,
              }),
            )
          } catch (error) {
            if (!isEndpointUnavailable(error)) throw error
          }
        }
        return store.get().lodging
      },
    ),

  // Guarda los datos editados en el panel de Alojamiento.
  updateLodging: (patch) =>
    withLocalFallback(
      async () => {
        const updated = await request(E.updateLodging, { name: "updateLodging", body: patch })
        // El backend responde el alojamiento completo: lo adoptamos entero.
        actions.updateLodging(mergeLodging({ ...patch, ...updated }))
        return store.get().lodging
      },
      async () => {
        actions.updateLodging(patch)
        return store.get().lodging
      },
    ),

  // Marca del alojamiento (colores, forma del borde y patrón del enlace).
  updateBrand: (patch) =>
    withLocalFallback(
      async () => {
        await request(E.updateBrand, { name: "updateBrand", body: patch })
        actions.updateBrand(patch)
        return store.get().lodging.brand
      },
      async () => {
        actions.updateBrand(patch)
        return store.get().lodging.brand
      },
    ),

  setLogo: (dataUrl) =>
    withLocalFallback(
      async () => {
        await request(E.setLogo, { name: "setLogo", body: { image: dataUrl } })
        actions.setLogo(dataUrl)
        return dataUrl
      },
      async () => {
        actions.setLogo(dataUrl)
        return dataUrl
      },
    ),

  // Fotos del alojamiento: máximo MAX_IMAGES; la primera es la portada.
  addImage: (dataUrl) =>
    withLocalFallback(
      async () => {
        const lodging = await request(E.addImage, { name: "addImage", body: { image: dataUrl } })
        if (Array.isArray(lodging?.images)) actions.updateLodging({ images: lodging.images.slice(0, MAX_IMAGES) })
        else actions.addImage(dataUrl)
        return store.get().lodging.images
      },
      async () => {
        actions.addImage(dataUrl)
        return store.get().lodging.images
      },
    ),

  removeImage: (index) =>
    withLocalFallback(
      async () => {
        const lodging = await request(pathFor(E.removeImage, { index }), { name: "removeImage" })
        if (Array.isArray(lodging?.images)) actions.updateLodging({ images: lodging.images.slice(0, MAX_IMAGES) })
        else actions.removeImage(index)
        return store.get().lodging.images
      },
      async () => {
        actions.removeImage(index)
        return store.get().lodging.images
      },
    ),

  // ── Disponibilidad ──────────────────────────────────────────────────────
  // El backend devuelve un array de fechas ISO ("YYYY-MM-DD") o { dates: [] }.
  getBlockedDays: () =>
    withLocalFallback(
      async () => {
        const response = await request(E.getBlockedDays, { name: "getBlockedDays" })
        const days = Array.isArray(response) ? response : response?.dates || []
        actions.setBlockedDays(days)
        return days
      },
      async () => store.get().blockedDays || [],
    ),

  setBlockedDays: (dates) =>
    withLocalFallback(
      async () => {
        await request(E.setBlockedDays, { name: "setBlockedDays", body: { dates } })
        actions.setBlockedDays(dates)
        return dates
      },
      async () => {
        actions.setBlockedDays(dates)
        return dates
      },
    ),

  // Alterna un día: el estado actual sale del store y se envía al backend,
  // que responde con la lista completa resultante.
  toggleBlockedDay: (date) =>
    withLocalFallback(
      async () => {
        const blocked = !(store.get().blockedDays || []).includes(date)
        const response = await request(pathFor(E.toggleBlockedDay, { date }), {
          name: "toggleBlockedDay",
          body: { date, blocked },
        })
        const days = Array.isArray(response) ? response : response?.dates
        if (Array.isArray(days)) actions.setBlockedDays(days)
        else actions.toggleBlockedDay(date)
        return store.get().blockedDays
      },
      async () => {
        actions.toggleBlockedDay(date)
        return store.get().blockedDays
      },
    ),

  // ── Reservas ────────────────────────────────────────────────────────────
  getBookings: () =>
    withLocalFallback(
      async () => {
        const response = await request(E.getBookings, { name: "getBookings" })
        if (Array.isArray(response)) actions.setBookings(response.map(toBooking))
        return store.get().bookings
      },
      async () => store.get().bookings || [],
    ),

  createBooking: (data) =>
    withLocalFallback(
      async () => {
        const created = await request(E.createBooking, { name: "createBooking", body: data })
        const booking = toBooking(created)
        actions.setBookings([...store.get().bookings, booking])
        return booking
      },
      async () => actions.createBooking(data),
    ),

  updateBooking: (id, patch) =>
    withLocalFallback(
      async () => {
        const updated = await request(pathFor(E.updateBooking, { id }), {
          name: "updateBooking",
          body: patch,
        })
        actions.updateBooking(id, updated || patch)
        return store.get().bookings.find((b) => String(b.id) === String(id))
      },
      async () => {
        actions.updateBooking(id, patch)
        return store.get().bookings.find((b) => String(b.id) === String(id))
      },
    ),

  setBookingStatus: (id, status) =>
    withLocalFallback(
      async () => {
        await request(pathFor(E.setBookingStatus, { id }), {
          name: "setBookingStatus",
          body: { status },
        })
        actions.setBookingStatus(id, status)
        return status
      },
      async () => {
        actions.setBookingStatus(id, status)
        return status
      },
    ),

  // Estado del pago de una reserva: "PENDING" (pendiente) | "PAID" (pagado).
  setPaymentStatus: (id, paymentStatus) =>
    withLocalFallback(
      async () => {
        await request(pathFor(E.setPaymentStatus, { id }), {
          name: "setPaymentStatus",
          body: { paymentStatus },
        })
        actions.setPaymentStatus(id, paymentStatus)
        return paymentStatus
      },
      async () => {
        actions.setPaymentStatus(id, paymentStatus)
        return paymentStatus
      },
    ),

  // Renueva el enlace de llegada del huésped: nueva vigencia de 24 h.
  regenerateOnboardingLink: (id) =>
    withLocalFallback(
      async () => {
        const response = await request(pathFor(E.regenerateOnboardingLink, { id }), {
          name: "regenerateOnboardingLink",
        })
        const issuedAt = response?.onboardingLinkIssuedAt || response?.issuedAt || new Date().toISOString()
        actions.updateBooking(id, { onboardingLinkIssuedAt: issuedAt })
        return issuedAt
      },
      async () => actions.regenerateOnboardingLink(id),
    ),

  // ── Onboarding del huésped (B2C) ────────────────────────────────────────
  // Lectura PÚBLICA del enlace de llegada: el huésped no tiene token, así que
  // la llamada va sin Authorization. Hidrata el store con el alojamiento, la
  // reserva, los servicios habilitados y las formas de pago activas, de modo
  // que la ficha (y la copia de credenciales de pago) se vea igual en el
  // celular del huésped que en el panel del anfitrión.
  getOnboarding: (lodgingId, bookingId) =>
    withLocalFallback(
      async () => {
        const data = await request(pathFor(E.getOnboarding, { lodgingId, bookingId }), {
          name: "getOnboarding",
          auth: false,
        })
        if (data?.lodging) actions.updateLodging(mergeLodging(data.lodging))
        if (data?.booking) actions.updateBooking(bookingId, toBooking(data.booking))
        if (Array.isArray(data?.services)) actions.setServices(data.services)
        if (data?.payments) {
          actions.setPayments({
            primary: data.payments.primary,
            wallets: data.payments.wallets,
            methods: data.payments.methods,
            bankDetails: data.payments.bankDetails,
          })
        }
        return data
      },
      async () => null,
    ),

  // El enlace público trae lodgingId y bookingId; el panel llama solo con el
  // bookingId, así que el alojamiento sale del store.
  submitOnboarding: (id, data) =>
    withLocalFallback(
      async () => {
        const lodgingId = store.get().lodging?.id
        await request(pathFor(E.submitOnboarding, { lodgingId, bookingId: id }), {
          name: "submitOnboarding",
          body: data,
        })
        actions.updateBooking(id, {
          guestName: data.guestName,
          guestEmail: data.guestEmail,
          guestPhone: data.guestPhone,
          selectedServices: Array.isArray(data.selectedServices) ? data.selectedServices : [],
          onboarding: { ...data, completedAt: new Date().toISOString() },
        })
        return data
      },
      async () => {
        actions.submitOnboarding(id, data)
        return data
      },
    ),

  // Solicitud de cotización de lancha: genera el JSON; NO lo envía a otra API.
  createBoatQuoteRequest: (id, quote) =>
    withLocalFallback(
      async () => {
        const created = await request(pathFor(E.createBoatQuoteRequest, { id }), {
          name: "createBoatQuoteRequest",
          body: quote,
        })
        actions.updateBooking(id, { boatQuote: created })
        return created
      },
      async () => actions.createBoatQuoteRequest(id, quote),
    ),

  declineBoatQuote: (id) =>
    withLocalFallback(
      async () => {
        await request(pathFor(E.declineBoatQuote, { id }), { name: "declineBoatQuote" })
        actions.declineBoatQuote(id)
      },
      async () => actions.declineBoatQuote(id),
    ),

  // ── Servicios adicionales ───────────────────────────────────────────────
  getServices: () =>
    withLocalFallback(
      async () => {
        const response = await request(E.getServices, { name: "getServices" })
        if (Array.isArray(response)) actions.setServices(response)
        return store.get().services
      },
      async () => store.get().services || [],
    ),

  addService: (data) =>
    withLocalFallback(
      async () => {
        const created = await request(E.addService, { name: "addService", body: data })
        actions.setServices([...store.get().services, created])
        return created
      },
      async () => actions.addService(data),
    ),

  updateService: (id, patch) =>
    withLocalFallback(
      async () => {
        const updated = await request(pathFor(E.updateService, { id }), {
          name: "updateService",
          body: patch,
        })
        actions.updateService(id, updated || patch)
        return store.get().services.find((sv) => String(sv.id) === String(id))
      },
      async () => {
        actions.updateService(id, patch)
        return store.get().services.find((sv) => String(sv.id) === String(id))
      },
    ),

  removeService: (id) =>
    withLocalFallback(
      async () => {
        await request(pathFor(E.removeService, { id }), { name: "removeService" })
        actions.removeService(id)
      },
      async () => actions.removeService(id),
    ),

  toggleService: (id) =>
    withLocalFallback(
      async () => {
        await request(pathFor(E.toggleService, { id }), { name: "toggleService" })
        actions.toggleService(id)
      },
      async () => actions.toggleService(id),
    ),

  // ── Pagos ───────────────────────────────────────────────────────────────
  // El backend devuelve el agregado completo del alojamiento.
  getPayments: () =>
    withLocalFallback(
      async () => {
        const response = await request(E.getPayments, { name: "getPayments" })
        if (response && typeof response === "object") {
          actions.setPayments({
            primary: response.primary,
            wallets: Array.isArray(response.wallets) ? response.wallets : undefined,
            // Límite estricto también del lado del cliente.
            methods: Array.isArray(response.methods)
              ? response.methods.slice(0, MAX_PAYMENT_METHODS)
              : undefined,
            bankDetails: response.bankDetails,
          })
        }
        return store.get().payments
      },
      async () => store.get().payments || {},
    ),

  setPrimaryCurrency: (code) =>
    withLocalFallback(
      async () => {
        await request(E.setPrimaryCurrency, { name: "setPrimaryCurrency", body: { currency: code } })
        actions.setPrimaryCurrency(code)
        return code
      },
      async () => {
        actions.setPrimaryCurrency(code)
        return code
      },
    ),

  toggleWallet: (code) =>
    withLocalFallback(
      async () => {
        await request(E.toggleWallet, { name: "toggleWallet", body: { currency: code } })
        actions.toggleWallet(code)
        return store.get().payments.wallets
      },
      async () => {
        actions.toggleWallet(code)
        return store.get().payments.wallets
      },
    ),

  togglePaymentMethod: (id) =>
    withLocalFallback(
      async () => {
        const current = store.get().payments.methods.find((m) => String(m.id) === String(id))
        const response = await request(pathFor(E.togglePaymentMethod, { id }), {
          name: "togglePaymentMethod",
          body: { enabled: !current?.enabled },
        })
        actions.updatePaymentMethod(id, { enabled: response?.enabled ?? !current?.enabled })
        return store.get().payments.methods
      },
      async () => {
        actions.togglePaymentMethod(id)
        return store.get().payments.methods
      },
    ),

  // Alta respetando el tope de MAX_PAYMENT_METHODS: si ya está lleno no se
  // llama al backend (evita un 400 que la pantalla tendría que traducir).
  addPaymentMethod: (data) => {
    if (store.get().payments.methods.length >= MAX_PAYMENT_METHODS) {
      const error = new Error(`Podés configurar hasta ${MAX_PAYMENT_METHODS} métodos de pago.`)
      error.status = 409
      throw error
    }
    return withLocalFallback(
      async () => {
        const created = await request(E.addPaymentMethod, { name: "addPaymentMethod", body: data })
        actions.setPayments({ methods: [...store.get().payments.methods, created].slice(0, MAX_PAYMENT_METHODS) })
        return created
      },
      async () => actions.addPaymentMethod(data),
    )
  },

  updatePaymentMethod: (id, patch) =>
    withLocalFallback(
      async () => {
        const updated = await request(pathFor(E.updatePaymentMethod, { id }), {
          name: "updatePaymentMethod",
          body: patch,
        })
        actions.updatePaymentMethod(id, updated || patch)
        return store.get().payments.methods.find((m) => String(m.id) === String(id))
      },
      async () => {
        actions.updatePaymentMethod(id, patch)
        return store.get().payments.methods.find((m) => String(m.id) === String(id))
      },
    ),

  removePaymentMethod: (id) =>
    withLocalFallback(
      async () => {
        await request(pathFor(E.removePaymentMethod, { id }), { name: "removePaymentMethod" })
        actions.removePaymentMethod(id)
        return store.get().payments.methods
      },
      async () => {
        actions.removePaymentMethod(id)
        return store.get().payments.methods
      },
    ),

  // Datos bancarios del alojamiento (transferencia externa).
  updateBankDetails: (patch) =>
    withLocalFallback(
      async () => {
        await request(E.updateBankDetails, { name: "updateBankDetails", body: patch })
        actions.updateBankDetails(patch)
        return store.get().payments.bankDetails
      },
      async () => {
        actions.updateBankDetails(patch)
        return store.get().payments.bankDetails
      },
    ),

  // ── Módulo de administración (solo ROLE_ADMIN) ─────────────────────────
  // Métricas agregadas del panel: si /admin/stats todavía no existe se
  // calculan con el listado de dueños que el backend sí expone.
  getAdminStats: () =>
    withLocalFallback(
      async () => {
        const stats = await request(E.getAdminStats, { name: "getAdminStats" })
        return {
          lodgings: Number(stats?.lodgings ?? 0),
          active: Number(stats?.active ?? 0),
          paused: Number(stats?.paused ?? 0),
          hosts: Number(stats?.hosts ?? 0),
          bookings: Number(stats?.bookings ?? 0),
        }
      },
      async () => {
        // Se calculan sobre el listado REAL de dueños (no sobre el seed de la
        // demo) para que las métricas coincidan con lo que muestra la tabla.
        let lodgings = store.get().admin?.lodgings || []
        try {
          const owners = await request(E.getAdminLodgings, { name: "getAllOwners" })
          if (Array.isArray(owners)) {
            lodgings = owners.map(toAdminLodging)
            actions.setAdminLodgings(lodgings)
          }
        } catch (error) {
          if (!isEndpointUnavailable(error)) throw error
        }
        return {
          lodgings: lodgings.length,
          active: lodgings.filter((l) => l.status === "ACTIVE").length,
          paused: lodgings.filter((l) => l.status === "PAUSED").length,
          hosts: new Set(lodgings.map((l) => l.ownerEmail)).size,
          bookings: 0,
        }
      },
    ),

  getAdminLodgings: () =>
    withLocalFallback(
      async () => {
        const owners = await request(E.getAdminLodgings, { name: "getAllOwners" })
        const list = Array.isArray(owners) ? owners.map(toAdminLodging) : []
        actions.setAdminLodgings(list)
        return list
      },
      async () => store.get().admin?.lodgings || [],
    ),

  // Pausa o reactiva un alojamiento habilitando o deshabilitando su dueño.
  setAdminLodgingStatus: (id, status) =>
    withLocalFallback(
      async () => {
        const enabled = status === "ACTIVE"
        const updated = await request(
          {
            ...pathFor(E.setAdminLodgingStatus, { id }),
            path: `${pathFor(E.setAdminLodgingStatus, { id }).path}?enabled=${enabled}`,
          },
          { name: "toggleOwnerStatus" },
        )
        const lodging = toAdminLodging(updated)
        actions.setAdminLodgingStatus(lodging.id, lodging.status)
        return lodging
      },
      async () => {
        actions.setAdminLodgingStatus(id, status)
        return { id, status, enabled: status === "ACTIVE" }
      },
    ),
}

// Adaptador REAL hacia el backend Java Spring Boot.
//
// Cada función corresponde a endpoints reales documentados en Swagger y endpoints.js.
// Mapea los DTOs del backend al formato que los componentes esperan
// y mantiene sincronizado el store reactivo de la aplicación.

import { ENDPOINTS as E } from "./endpoints"
import { request } from "./httpClient"
import { actions, store } from "../state/store"

const withId = (endpoint, id) => ({
  ...endpoint,
  path: endpoint.path.replace("{id}", encodeURIComponent(id)),
})

// Convierte un dataUrl base64 a un File real para subidas multipart a Spring Boot
function toFile(fileOrDataUrl, defaultFilename = "upload.jpg") {
  if (!fileOrDataUrl) return null
  if (typeof File !== "undefined" && fileOrDataUrl instanceof File) return fileOrDataUrl
  if (typeof Blob !== "undefined" && fileOrDataUrl instanceof Blob) {
    return new File([fileOrDataUrl], defaultFilename, { type: fileOrDataUrl.type || "image/jpeg" })
  }
  if (typeof fileOrDataUrl === "string" && fileOrDataUrl.startsWith("data:")) {
    const parts = fileOrDataUrl.split(",")
    const mimeMatch = parts[0].match(/:(.*?);/)
    const mime = mimeMatch ? mimeMatch[1] : "image/jpeg"
    const bstr = atob(parts[1])
    let n = bstr.length
    const u8arr = new Uint8Array(n)
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n)
    }
    return new File([u8arr], defaultFilename, { type: mime })
  }
  return null
}

// AuthResponse → usuario interno de la app
const toUser = (response) => {
  const roles = response?.roles || []
  return {
    id: response?.userId ?? null,
    name: response?.fullName ?? "",
    email: response?.email ?? "",
    roles,
    role: roles.some((r) => /ADMIN/i.test(r)) ? "ADMIN" : "HOST",
  }
}

const toAuth = (response) => ({
  token: response?.token ?? null,
  tokenType: response?.tokenType ?? "Bearer",
  user: toUser(response),
})

// Mapeo AccommodationProfileResponse → store lodging
const toFrontendLodging = (profile, fallbackName = "Mi alojamiento") => {
  if (!profile) return null
  const images = Array.isArray(profile.images)
    ? profile.images.map((img) => (typeof img === "string" ? img : img.imageUrl))
    : []

  return {
    id: profile.id ? String(profile.id) : "lod_1",
    rawId: profile.id,
    name: profile.name || fallbackName,
    tagline: profile.welcomeMessage || "Descanso frente al río",
    location: profile.address || "Tigre, Buenos Aires",
    phone: profile.phone || "+54 9 11 5555 0101",
    rooms: profile.roomCount ?? 1,
    bathrooms: profile.bathroomCount ?? 1,
    pricePerNight: profile.basePrice ? Number(profile.basePrice) : 85000,
    currency: "ARS",
    images,
    imageObjects: profile.images || [],
    logo: profile.logoUrl || null,
    brand: {
      primary: profile.primaryColor || "#0fa3a3",
      accent: profile.accentColor || "#d4f53c",
      radius: profile.backgroundPattern || "rounded",
    },
  }
}

// Mapeo ReservationResponse → store booking
const toFrontendBooking = (r) => ({
  id: String(r.id),
  rawId: r.id,
  confirmationCode: r.confirmationCode,
  guestName: r.guestFullName || "Huésped",
  guestEmail: r.guestEmail || "",
  checkIn: r.checkIn ? String(r.checkIn).slice(0, 10) : "",
  checkOut: r.checkOut ? String(r.checkOut).slice(0, 10) : "",
  guests: r.guestCount || 1,
  total: r.totalAmount ? Number(r.totalAmount) : 0,
  currency: "ARS",
  status: r.status, // "CONFIRMED" | "PENDING" | "CANCELLED" | "EXPIRED" | "COMPLETED"
  additional: r.specialRequests || "",
})

// Mapeo OwnerResponse → store admin.lodging
const toFrontendAdminLodging = (o) => ({
  id: String(o.id),
  name: o.accommodationName || "Alojamiento",
  owner: o.fullName || "Dueño",
  ownerEmail: o.email,
  rooms: "-",
  status: o.enabled ? "ACTIVE" : "PAUSED",
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

  forgotPassword: (email) =>
    request(E.forgotPassword, { name: "forgotPassword", body: { email }, auth: false }),

  resetPassword: ({ email, code, newPassword }) =>
    request(E.resetPassword, {
      name: "resetPassword",
      body: { email, code, newPassword },
      auth: false,
    }),

  // ── Alojamiento y Perfil ────────────────────────────────────────────────
  getLodging: async () => {
    try {
      const profile = await request(E.getAccommodationProfile, { name: "getAccommodationProfile" })
      const lodging = toFrontendLodging(profile)
      if (lodging) {
        actions.updateLodging(lodging)
      }
      return lodging
    } catch (err) {
      // 404 significa que el owner aún no guardó su perfil inicial
      if (err?.status === 404) {
        return store.get().lodging
      }
      throw err
    }
  },

  updateLodging: async (patch) => {
    const current = store.get().lodging || {}
    const body = {}

    if (patch.name !== undefined) body.name = patch.name
    if (patch.rooms !== undefined) body.roomCount = Number(patch.rooms)
    if (patch.bathrooms !== undefined) body.bathroomCount = Number(patch.bathrooms)
    if (patch.pricePerNight !== undefined) body.basePrice = Number(patch.pricePerNight)
    if (patch.location !== undefined) body.address = patch.location
    if (patch.phone !== undefined) body.phone = patch.phone
    if (patch.tagline !== undefined) body.welcomeMessage = patch.tagline

    if (patch.brand?.primary) body.primaryColor = patch.brand.primary
    if (patch.brand?.accent) body.accentColor = patch.brand.accent
    if (patch.brand?.radius) body.backgroundPattern = patch.brand.radius

    let profile
    try {
      profile = await request(E.updateAccommodationProfile, {
        name: "updateAccommodationProfile",
        body,
      })
    } catch (err) {
      if (err?.status === 404) {
        // Si no existía perfil, lo creamos con POST
        profile = await request(E.createAccommodationProfile, {
          name: "createAccommodationProfile",
          body: {
            name: patch.name || current.name || "Mi alojamiento",
            roomCount: patch.rooms ?? current.rooms ?? 1,
            bathroomCount: patch.bathrooms ?? current.bathrooms ?? 1,
            guestCapacity: (patch.rooms ?? current.rooms ?? 1) * 2,
            basePrice: patch.pricePerNight ?? current.pricePerNight ?? 85000,
            primaryColor: patch.brand?.primary || current.brand?.primary || "#0fa3a3",
            accentColor: patch.brand?.accent || current.brand?.accent || "#d4f53c",
            backgroundPattern: patch.brand?.radius || current.brand?.radius || "rounded",
            welcomeMessage: patch.tagline || current.tagline || "",
            address: patch.location || current.location || "",
            phone: patch.phone || current.phone || "",
          },
        })
      } else {
        throw err
      }
    }

    const updated = toFrontendLodging(profile, current.name)
    if (updated) {
      actions.updateLodging(updated)
    }
    return updated
  },

  setLogo: async (fileOrDataUrl) => {
    if (!fileOrDataUrl) {
      // Eliminar o limpiar logo
      await request(E.updateAccommodationProfile, {
        name: "updateAccommodationProfile",
        body: { logoUrl: "" },
      })
      actions.setLogo(null)
      return null
    }

    const file = toFile(fileOrDataUrl, "logo.png")
    if (!file) return null

    const formData = new FormData()
    formData.append("file", file)

    const profile = await request(E.uploadAccommodationLogo, {
      name: "uploadAccommodationLogo",
      body: formData,
    })

    const newLogo = profile?.logoUrl || null
    actions.setLogo(newLogo)
    return newLogo
  },

  addImage: async (fileOrDataUrl) => {
    const file = toFile(fileOrDataUrl, "gallery-image.jpg")
    if (!file) return null

    const formData = new FormData()
    formData.append("file", file)

    const imageResponse = await request(E.uploadAccommodationGalleryImage, {
      name: "uploadAccommodationGalleryImage",
      body: formData,
    })

    const url = imageResponse?.imageUrl
    if (url) {
      actions.addImage(url)
    }
    return imageResponse
  },

  removeImage: async (index) => {
    const currentLodging = store.get().lodging
    const imageObj = currentLodging?.imageObjects?.[index]
    const imageId = imageObj?.id

    if (imageId) {
      await request(withId(E.deleteAccommodationGalleryImage, imageId), {
        name: "deleteAccommodationGalleryImage",
      })
    }
    actions.removeImage(index)
  },

  updateBrand: async (patch) => {
    actions.updateBrand(patch)
    const body = {}
    if (patch.primary) body.primaryColor = patch.primary
    if (patch.accent) body.accentColor = patch.accent
    if (patch.radius) body.backgroundPattern = patch.radius

    return request(E.updateAccommodationProfile, {
      name: "updateAccommodationProfile",
      body,
    }).catch(() => null)
  },

  // ── Disponibilidad ──────────────────────────────────────────────────────
  getBlockedDays: async () => {
    try {
      const history = await request(E.getAccommodationDisableHistory, {
        name: "getAccommodationDisableHistory",
      })
      // Extraemos las fechas de los rangos de desactivaciones activas
      const blocked = []
      if (Array.isArray(history)) {
        history.forEach((d) => {
          if (d.disabledFrom) {
            const from = String(d.disabledFrom).slice(0, 10)
            const until = d.disabledUntil ? String(d.disabledUntil).slice(0, 10) : from
            blocked.push(from)
            if (until && until !== from) blocked.push(until)
          }
        })
      }
      const unique = Array.from(new Set(blocked))
      actions.setBlockedDays(unique)
      return unique
    } catch {
      return store.get().blockedDays
    }
  },

  toggleBlockedDay: async (dateIso) => {
    const currentBlocked = store.get().blockedDays || []
    const isCurrentlyBlocked = currentBlocked.includes(dateIso)

    if (isCurrentlyBlocked) {
      // Intentar reactivar
      try {
        await request(E.enableAccommodation, { name: "enableAccommodation" })
      } catch {
        /* continuamos para reflejar en el store */
      }
    } else {
      // Desactivar temporalmente
      try {
        await request(E.disableAccommodation, {
          name: "disableAccommodation",
          body: {
            reason: "Bloqueado desde calendario",
            disabledFrom: `${dateIso}T00:00:00`,
            disabledUntil: `${dateIso}T23:59:59`,
          },
        })
      } catch {
        /* continuamos para reflejar en el store */
      }
    }
    actions.toggleBlockedDay(dateIso)
  },

  setBlockedDays: (dates) => {
    actions.setBlockedDays(dates)
  },

  // ── Reservas ────────────────────────────────────────────────────────────
  getBookings: async () => {
    try {
      const data = await request(E.getReservations, { name: "getReservations" })
      const bookings = Array.isArray(data) ? data.map(toFrontendBooking) : []
      actions.setState?.((s) => ({ ...s, bookings })) ||
        store.get().bookings.splice(0, store.get().bookings.length, ...bookings)
      return bookings
    } catch (err) {
      if (err?.status === 404) return []
      throw err
    }
  },

  setBookingStatus: async (id, status) => {
    actions.setBookingStatus(id, status)
    if (status === "CANCELLED") {
      await request(
        { ...withId(E.cancelReservation, id), path: `${withId(E.cancelReservation, id).path}?reason=Cancelada%20por%20anfitrión` },
        { name: "cancelReservation" },
      ).catch(() => null)
    } else if (status === "COMPLETED") {
      await request(withId(E.completeReservation, id), { name: "completeReservation" }).catch(() => null)
    }
  },

  // ── Pagos y Monedas ─────────────────────────────────────────────────────
  getPayments: async () => {
    try {
      const [methods, currencies] = await Promise.all([
        request(E.getAccommodationPaymentMethods, { name: "getAccommodationPaymentMethods" }).catch(() => []),
        request(E.getAccommodationCurrencies, { name: "getAccommodationCurrencies" }).catch(() => []),
      ])

      const enabledCurrencies = Array.isArray(currencies)
        ? currencies.filter((c) => c.enabled).map((c) => c.currency)
        : []

      const mappedMethods = Array.isArray(methods)
        ? methods.map((m) => ({
            id: String(m.id),
            rawId: m.id,
            type: m.paymentType === "BANK_TRANSFER" ? "CARD" : "WALLET",
            label: m.displayName || m.providerName || "Método de pago",
            enabled: m.isActive !== false,
            isDefault: Boolean(m.isDefault),
          }))
        : []

      const payments = {
        primary: "ARS",
        wallets: enabledCurrencies.length ? enabledCurrencies : ["USD", "EUR"],
        methods: mappedMethods.length ? mappedMethods : store.get().payments.methods,
      }

      return payments
    } catch {
      return store.get().payments
    }
  },

  setPrimaryCurrency: async (code) => {
    actions.setPrimaryCurrency(code)
    actions.updateLodging({ currency: code })
  },

  toggleWallet: async (code) => {
    actions.toggleWallet(code)
    const has = store.get().payments.wallets.includes(code)
    await request(E.upsertAccommodationCurrency, {
      name: "upsertAccommodationCurrency",
      body: {
        currency: code,
        enabled: has,
        useAutomaticRate: true,
      },
    }).catch(() => null)
  },

  togglePaymentMethod: async (id) => {
    actions.togglePaymentMethod(id)
    const method = store.get().payments.methods.find((m) => m.id === id)
    if (method) {
      await request(withId(E.updateAccommodationPaymentMethod, id), {
        name: "updateAccommodationPaymentMethod",
        body: { isActive: method.enabled },
      }).catch(() => null)
    }
  },

  addPaymentMethod: async (label, type = "WALLET") => {
    actions.addPaymentMethod(label, type)
    await request(E.createAccommodationPaymentMethod, {
      name: "createAccommodationPaymentMethod",
      body: {
        displayName: label,
        paymentType: type === "CARD" ? "CARD" : "DIGITAL_WALLET",
        isActive: true,
      },
    }).catch(() => null)
  },

  // ── Métricas Dashboard ──────────────────────────────────────────────────
  getOwnerMetrics: () => request(E.getOwnerMetrics, { name: "getOwnerMetrics" }),
  getAdminMetrics: () => request(E.getAdminMetrics, { name: "getAdminMetrics" }),

  // ── Admin ───────────────────────────────────────────────────────────────
  getAdminLodgings: async () => {
    const owners = await request(E.getAllOwners, { name: "getAllOwners" })
    const lodgings = Array.isArray(owners) ? owners.map(toFrontendAdminLodging) : []
    return lodgings
  },

  setAdminLodgingStatus: async (id, status) => {
    actions.setAdminLodgingStatus(id, status)
    return request(
      { ...withId(E.toggleOwnerStatus, id), path: `${withId(E.toggleOwnerStatus, id).path}?enabled=${status === "ACTIVE"}` },
      { name: "toggleOwnerStatus" },
    )
  },

  getAllOwners: () => request(E.getAllOwners, { name: "getAllOwners" }),
  getOwnerById: (id) => request(withId(E.getOwnerById, id), { name: "getOwnerById" }),
  getOwnerByEmail: (email) =>
    request(
      { ...E.getOwnerByEmail, path: E.getOwnerByEmail.path.replace("{email}", encodeURIComponent(email)) },
      { name: "getOwnerByEmail" },
    ),
  toggleOwnerStatus: (id, enabled) =>
    request(
      { ...withId(E.toggleOwnerStatus, id), path: `${withId(E.toggleOwnerStatus, id).path}?enabled=${enabled}` },
      { name: "toggleOwnerStatus" },
    ),
}

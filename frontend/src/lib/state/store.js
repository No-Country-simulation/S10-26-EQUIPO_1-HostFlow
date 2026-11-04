// Estado de aplicación en memoria (modo demo).
//
// Es la fuente de verdad del frontend cuando `VITE_API_URL` no está definida.
// Expone el MISMO contrato que el backend (ver docs/swagger-api.md): cuando se
// conecte el backend real, `src/lib/api/index.js` pasará a usar `httpApi` y este
// archivo se usa solo como referencia de datos de ejemplo.
//
// Persistencia: localStorage (compartido por TODAS las pestañas y ventanas de
// este navegador, como una "base de datos local" de la demo). Así el enlace de
// llegada abierto en otra pestaña ve los cambios que hizo el anfitrión.
// En OTRO navegador/equipo no viajan: eso recién existe con backend real.
//
// Este archivo solo contiene el núcleo pub/sub y las acciones. Los datos de
// ejemplo viven en seedData.js, las migraciones en normalizeState.js, la
// persistencia en persistence.js y las reglas de dominio en lib/domain.

import { MAX_IMAGES } from "@lib/domain/lodging";
import { MAX_PAYMENT_METHODS } from "@lib/domain/payments";
import { onboardingLinkIssuedAt } from "@lib/domain/onboarding";
import { buildBoatQuotePayload } from "./boatQuotePayload";
import { loadState, persistState, watchStorage } from "./persistence";
import { seedLodging } from "./seedData";

// ── Núcleo pub/sub ──────────────────────────────────────────────────────────

let state = loadState();
const listeners = new Set();

function emit() {
  persistState(state);
  listeners.forEach((listener) => listener(state));
}

function setState(updater) {
  state = typeof updater === "function" ? updater(state) : { ...state, ...updater };
  emit();
}

function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

// Otra pestaña de este navegador cambió el estado: se adopta sin re-persistir
// (ver persistence.js) para que el enlace de llegada se mantenga al día.
watchStorage((next) => {
  state = next;
  listeners.forEach((listener) => listener(state));
});

export const store = {
  get: () => state,
  subscribe,
};

// ── Acciones (demo) ─────────────────────────────────────────────────────────

const delay = (ms = 320) => new Promise((resolve) => setTimeout(resolve, ms));

const publicUser = ({ password, ...user }) => user;

export const actions = {
  // Setea (o limpia) el usuario de la sesión.
  // Con el backend real es el login quien lo informa: el token viaja en los headers
  // pero el store necesita la identidad para proteger las rutas de la interfaz.
  setUser(user) {
    setState((s) => ({ ...s, user: user ? { ...user } : null }))
  },

  async login({ email, password }) {
    await delay();
    const user = state.users.find((u) => u.email.toLowerCase() === String(email).toLowerCase());
    if (!user || user.password !== password) {
      const error = new Error("Correo o contraseña incorrectos.");
      error.status = 401;
      throw error;
    }
    setState((s) => ({ ...s, user: publicUser(user) }));
    return { user: publicUser(user) };
  },

  // Acceso rápido de demostración: ignora la contraseña (solo modo demo),
  // para que la presentación nunca se bloquee tras cambiar una clave.
  async demoLogin(email) {
    await delay(200);
    const user = state.users.find((u) => u.email.toLowerCase() === String(email).toLowerCase());
    if (!user) {
      const error = new Error("No existe el usuario de demostración.");
      error.status = 404;
      throw error;
    }
    setState((s) => ({ ...s, user: publicUser(user) }));
    return { user: publicUser(user) };
  },

  async register({ fullName, accommodationName, lodgingName, email, password }) {
    await delay();
    if (state.users.some((u) => u.email.toLowerCase() === String(email).toLowerCase())) {
      const error = new Error("El correo ya está registrado.");
      error.status = 409;
      throw error;
    }
    const name = accommodationName || lodgingName || "Mi alojamiento";
    const lodging = {
      ...seedLodging,
      id: `lod_${Date.now().toString(36)}`,
      name,
      images: [],
      logo: null,
    };
    const user = {
      id: `u_${Date.now().toString(36)}`,
      name: fullName,
      email,
      role: "HOST",
      lodgingId: lodging.id,
    };
    setState((s) => ({
      ...s,
      user,
      lodging,
      users: [...s.users, { ...user, password }],
    }));
    return { user };
  },

  // Equivale a POST /auth/forgot-password: genera el código OTP de 6 dígitos.
  async forgotPassword(email) {
    await delay()
    const code = String(Math.floor(100000 + Math.random() * 900000))
    setState((s) => ({ ...s, lastOtp: { email, code } }))
    return {
      message: "Si el correo está registrado, recibirás un código.",
      email,
      expirationMinutes: 60,
      demoCode: code,
    }
  },

  // Equivale a POST /auth/reset-password: { email, code, newPassword }.
  async resetPassword({ email, code, password }) {
    await delay()
    const valid = state.lastOtp?.email === email && state.lastOtp?.code === code
    if (!valid) {
      const error = new Error("El código es inválido o expiró.");
      error.status = 400;
      throw error;
    }
    setState((s) => ({
      ...s,
      lastOtp: null,
      users: s.users.map((u) =>
        u.email.toLowerCase() === String(email).toLowerCase() ? { ...u, password } : u,
      ),
    }))
    return { message: "Contraseña actualizada.", email, expirationMinutes: 60 }
  },

  logout() {
    setState((s) => ({ ...s, user: null, lastOtp: null }));
  },

  // ── Alojamiento ──────────────────────────────────────────────────────────
  getLodging() {
    return state.lodging;
  },

  updateLodging(patch) {
    setState((s) => ({ ...s, lodging: { ...s.lodging, ...patch } }));
  },

  setLogo(dataUrl) {
    actions.updateLodging({ logo: dataUrl });
  },

  addImage(dataUrl) {
    setState((s) => {
      // Tope de 4 fotos: el enlace de llegada muestra 0 a 4 y la primera es portada.
      if (s.lodging.images.length >= MAX_IMAGES) return s;
      return { ...s, lodging: { ...s.lodging, images: [...s.lodging.images, dataUrl] } };
    });
  },

  removeImage(index) {
    setState((s) => ({
      ...s,
      lodging: { ...s.lodging, images: s.lodging.images.filter((_, i) => i !== index) },
    }));
  },

  updateBrand(patch) {
    setState((s) => ({
      ...s,
      lodging: { ...s.lodging, brand: { ...s.lodging.brand, ...patch } },
    }));
  },

  // Reemplaza la colección completa de reservas. Lo usa el adaptador real
  // (httpApi) para adoptar lo que devuelve el backend; en modo demo no se usa.
  setBookings(bookings) {
    setState((s) => ({ ...s, bookings: bookings.map((b) => ({ ...b })) }));
  },

  // ── Disponibilidad ───────────────────────────────────────────────────────
  toggleBlockedDay(date) {
    setState((s) => {
      const has = s.blockedDays.includes(date);
      return {
        ...s,
        blockedDays: has ? s.blockedDays.filter((d) => d !== date) : [...s.blockedDays, date],
      };
    });
  },

  setBlockedDays(dates) {
    setState((s) => ({ ...s, blockedDays: dates }));
  },

  // ── Reservas ─────────────────────────────────────────────────────────────
  setBookingStatus(id, status) {
    setState((s) => ({
      ...s,
      bookings: s.bookings.map((b) => (b.id === id ? { ...b, status } : b)),
    }));
  },

  // Equivale a POST /lodgings/mine/bookings (pendiente en el backend).
  createBooking(data) {
    const booking = {
      id: `bk_${Date.now().toString(36)}`,
      guestName: String(data.guestName || "").trim(),
      guestEmail: String(data.guestEmail || "").trim(),
      guestPhone: String(data.guestPhone || "").trim(),
      checkIn: data.checkIn,
      checkOut: data.checkOut,
      guests: Number(data.guests) || 1,
      total: Number(data.total) || 0,
      currency: data.currency || state.lodging.currency,
      status: data.status || "PENDING",
      paymentStatus: "PENDING",
      additional: String(data.additional || "").trim(),
      selectedServices: [],
      // El enlace de llegada se emite al crear la reserva y dura 24 h.
      onboardingLinkIssuedAt: onboardingLinkIssuedAt(),
    };
    setState((s) => ({ ...s, bookings: [...s.bookings, booking] }));
    return booking;
  },

  // Renueva el enlace de llegada de una reserva: nueva marca de emisión →
  // vuelve a valer 24 h.
  regenerateOnboardingLink(id) {
    const issuedAt = onboardingLinkIssuedAt();
    setState((s) => ({
      ...s,
      bookings: s.bookings.map((b) => (b.id === id ? { ...b, onboardingLinkIssuedAt: issuedAt } : b)),
    }));
    return issuedAt;
  },

  // Equivale a PATCH /bookings/{id} (pendiente en el backend).
  updateBooking(id, patch) {
    setState((s) => ({
      ...s,
      bookings: s.bookings.map((b) => (b.id === id ? { ...b, ...patch } : b)),
    }));
  },

  // Estado del pago de una reserva: "PENDING" (pendiente) | "PAID" (pagado).
  setPaymentStatus(id, paymentStatus) {
    setState((s) => ({
      ...s,
      bookings: s.bookings.map((b) => (b.id === id ? { ...b, paymentStatus } : b)),
    }));
  },

  // ── Onboarding del huésped (B2C, sin cuenta) ─────────────────────────────
  // Guarda los datos completados por el huésped en el enlace de llegada.
  submitOnboarding(id, data) {
    setState((s) => ({
      ...s,
      bookings: s.bookings.map((b) =>
        b.id === id
          ? {
              ...b,
              guestName: data.guestName || b.guestName,
              guestEmail: data.guestEmail || b.guestEmail,
              guestPhone: data.guestPhone || b.guestPhone || "",
              selectedServices: Array.isArray(data.selectedServices) ? data.selectedServices : b.selectedServices || [],
              onboarding: { ...data, completedAt: new Date().toISOString() },
            }
          : b,
      ),
    }));
  },

  // Registra la solicitud de cotización de lancha con su payload JSON.
  // El JSON se GENERA aquí para que otra API pueda consumirlo después:
  // NO se envía a ningún endpoint (la integración externa está pendiente).
  createBoatQuoteRequest(id, quote) {
    const booking = state.bookings.find((b) => b.id === id);
    const payload = buildBoatQuotePayload(state.lodging, booking, quote);
    const request = {
      ...quote,
      id: `bq_${Date.now().toString(36)}`,
      status: "REQUESTED",
      createdAt: new Date().toISOString(),
      // Pendiente: el backend aún no expone un endpoint para notificar por correo
      // ni para reenviar la solicitud a la API de transporte.
      delivery: { email: "PENDING_BACKEND", forward: "PENDING_INTEGRATION" },
      payload,
    };
    setState((s) => ({
      ...s,
      bookings: s.bookings.map((b) => (b.id === id ? { ...b, boatQuote: request } : b)),
    }));
    return request;
  },

  // El huésped puede declinar la cotización: el onboarding continúa igual.
  declineBoatQuote(id) {
    setState((s) => ({
      ...s,
      bookings: s.bookings.map((b) =>
        b.id === id ? { ...b, boatQuote: { status: "DECLINED", createdAt: new Date().toISOString() } } : b,
      ),
    }));
  },

  // ── Pagos ────────────────────────────────────────────────────────────────
  // Sincronización con el backend: reemplaza SOLO las claves que vengan
  // definidas (primary / wallets / methods / bankDetails), sin tocar el resto.
  // El tope de métodos se respeta también acá por si el backend lo manda mal.
  setPayments(patch) {
    setState((s) => {
      const current = s.payments;
      const methods = Array.isArray(patch?.methods)
        ? patch.methods.slice(0, MAX_PAYMENT_METHODS).map((m) => ({ ...m, keys: { ...(m.keys || {}) } }))
        : current.methods;
      return {
        ...s,
        payments: {
          primary: patch?.primary ?? current.primary,
          wallets: Array.isArray(patch?.wallets) ? [...patch.wallets] : current.wallets,
          methods,
          bankDetails: patch?.bankDetails ? { ...current.bankDetails, ...patch.bankDetails } : current.bankDetails,
        },
      };
    });
  },

  setPrimaryCurrency(code) {
    setState((s) => ({ ...s, payments: { ...s.payments, primary: code } }));
  },

  toggleWallet(code) {
    setState((s) => {
      const has = s.payments.wallets.includes(code);
      return {
        ...s,
        payments: {
          ...s.payments,
          wallets: has ? s.payments.wallets.filter((c) => c !== code) : [...s.payments.wallets, code],
        },
      };
    });
  },

  togglePaymentMethod(id) {
    setState((s) => ({
      ...s,
      payments: {
        ...s.payments,
        methods: s.payments.methods.map((m) => (m.id === id ? { ...m, enabled: !m.enabled } : m)),
      },
    }));
  },

  addPaymentMethod(payload, maybeType = "WALLET") {
    const isObj = typeof payload === "object" && payload !== null;
    const label = isObj ? payload.label : payload;
    const type = isObj ? payload.type || "WALLET" : maybeType;
    const keys = isObj && payload.keys ? payload.keys : {};
    const notes = isObj ? payload.notes || "" : "";

    setState((s) => {
      if (s.payments.methods.length >= MAX_PAYMENT_METHODS) {
        return s; // Límite de 4 métodos
      }
      return {
        ...s,
        payments: {
          ...s.payments,
          methods: [
            ...s.payments.methods,
            {
              id: `pm_${Date.now().toString(36)}`,
              type,
              label: String(label || "Nuevo método").trim(),
              enabled: true,
              keys,
              notes,
            },
          ],
        },
      };
    });
  },

  updatePaymentMethod(id, patch) {
    setState((s) => ({
      ...s,
      payments: {
        ...s.payments,
        methods: s.payments.methods.map((m) =>
          m.id === id
            ? {
                ...m,
                ...patch,
                keys: patch.keys ? { ...m.keys, ...patch.keys } : m.keys,
              }
            : m,
        ),
      },
    }));
  },

  removePaymentMethod(id) {
    setState((s) => ({
      ...s,
      payments: {
        ...s.payments,
        methods: s.payments.methods.filter((m) => m.id !== id),
      },
    }));
  },

  // Datos bancarios del alojamiento (transferencia externa) que ve el huésped.
  updateBankDetails(patch) {
    setState((s) => ({
      ...s,
      payments: { ...s.payments, bankDetails: { ...s.payments.bankDetails, ...patch } },
    }));
  },

  // ── Servicios adicionales ────────────────────────────────────────────────
  // Reemplaza el listado completo (sincronización con el backend).
  setServices(services) {
    setState((s) => ({ ...s, services: services.map((sv) => ({ ...sv })) }));
  },

  addService({ name, price, description, pricing, perGuest }) {
    const service = {
      id: `sv_${Date.now().toString(36)}`,
      name: String(name || "").trim(),
      price: Number(price) || 0,
      pricing: pricing === "PER_NIGHT" ? "PER_NIGHT" : "FIXED",
      // Cobro por huésped: el importe se multiplica por la cantidad de personas
      // de la reserva (comidas, toallas, excursiones por persona).
      perGuest: Boolean(perGuest),
      description: String(description || "").trim(),
      enabled: true,
    };
    setState((s) => ({ ...s, services: [...s.services, service] }));
    return service;
  },

  updateService(id, patch) {
    setState((s) => ({
      ...s,
      services: s.services.map((sv) => (sv.id === id ? { ...sv, ...patch } : sv)),
    }));
  },

  removeService(id) {
    setState((s) => ({ ...s, services: s.services.filter((sv) => sv.id !== id) }));
  },

  toggleService(id) {
    setState((s) => ({
      ...s,
      services: s.services.map((sv) => (sv.id === id ? { ...sv, enabled: !sv.enabled } : sv)),
    }));
  },

  // ── Admin ────────────────────────────────────────────────────────────────
  getAdminLodgings() {
    return state.admin?.lodgings || [];
  },

  // Reemplaza el listado completo (sincronización con GET /owners).
  setAdminLodgings(lodgings) {
    setState((s) => ({
      ...s,
      admin: { lodgings: lodgings.map((l) => ({ ...l })) },
    }));
  },

  setAdminLodgingStatus(id, status) {
    setState((s) => ({
      ...s,
      admin: {
        lodgings: (s.admin?.lodgings || []).map((l) =>
          String(l.id) === String(id) ? { ...l, status, enabled: status === "ACTIVE" } : l,
        ),
      },
    }));
  },
};

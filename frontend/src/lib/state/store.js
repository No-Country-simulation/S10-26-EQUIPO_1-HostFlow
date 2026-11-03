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

import { addDays, toISODate } from "@utils/format";
import { normalizeHex } from "@utils/color";
import { DEFAULT_PATTERN, PATTERN_IDS } from "@lib/theme/patterns";

const STORAGE_KEY = "hostflow.app.v1";
const today = toISODate();

// ── Datos de ejemplo ────────────────────────────────────────────────────────

const seedUsers = [
  {
    id: "u_host",
    name: "Martina Silva",
    email: "anfitrion@hostflow.app",
    password: "demo1234",
    role: "HOST",
    lodgingId: "lod_1",
  },
  {
    id: "u_admin",
    name: "Equipo HostFlow",
    email: "admin@hostflow.app",
    password: "admin1234",
    role: "ADMIN",
    lodgingId: null,
  },
];

const seedLodging = {
  id: "lod_1",
  name: "Cabañas del Arroyo",
  tagline: "Descanso frente al río",
  location: "Tigre, Buenos Aires",
  phone: "+54 9 11 5555 0101",
  rooms: 4,
  bathrooms: 3,
  capacity: 10,
  pricePerNight: 85000,
  currency: "ARS",
  images: [],
  logo: null,
  // Paleta extraída del logo: se persiste con el alojamiento (sugerencias de color).
  brandPalette: [],
  // Marca del alojamiento: 3 colores con rol fijo (marca, acción y fondo),
  // la forma de las esquinas y el patrón de fondo del enlace de llegada.
  brand: { primary: "#0fa3a3", accent: "#d4f53c", tertiary: "#f4b8c4", radius: "rounded", pattern: "none" },
  // Enlace de llegada (onboarding B2C): se compone con lodging.id + booking.id.
  onboardingPath: "/llegada",
};

// Colores por defecto de la marca: se re-aplican al cargar un estado guardado
// por una versión anterior (que solo tenía primary/accent).
export const BRAND_DEFAULTS = {
  primary: "#0fa3a3",
  accent: "#d4f53c",
  tertiary: "#f4b8c4",
  radius: "rounded",
  pattern: DEFAULT_PATTERN,
};

// Valores válidos de la forma de la marca (los inválidos vuelven al default).
const BRAND_RADIUS = ["rounded", "soft", "sharp", "pill"];

// Fotos del alojamiento: opcionales, de 0 a 4 (la primera es la portada).
export const MAX_IMAGES = 4;

// Sanitiza la marca guardada: normaliza cada hex y valida la forma y el patrón,
// para que un estado corrupto o de una versión vieja nunca deje valores rotos.
function sanitizeBrand(brand) {
  return {
    primary: normalizeHex(brand?.primary, BRAND_DEFAULTS.primary),
    accent: normalizeHex(brand?.accent, BRAND_DEFAULTS.accent),
    tertiary: normalizeHex(brand?.tertiary, BRAND_DEFAULTS.tertiary),
    radius: BRAND_RADIUS.includes(brand?.radius) ? brand.radius : BRAND_DEFAULTS.radius,
    pattern: PATTERN_IDS.includes(brand?.pattern) ? brand.pattern : BRAND_DEFAULTS.pattern,
  };
}

// Vigencia del enlace de llegada (spec: dura 24 h desde su emisión).
export const ONBOARDING_LINK_TTL_MS = 24 * 60 * 60 * 1000;
export const onboardingLinkIssuedAt = () => new Date().toISOString();
export function isOnboardingLinkExpired(booking) {
  const issued = booking?.onboardingLinkIssuedAt ? Date.parse(booking.onboardingLinkIssuedAt) : NaN;
  if (Number.isNaN(issued)) return false; // reservas antiguas sin marca: sin expiración
  return Date.now() - issued > ONBOARDING_LINK_TTL_MS;
}

const seedBookings = [
  {
    id: "bk_1",
    guestName: "María González",
    guestEmail: "maria@example.com",
    guestPhone: "+54 9 11 5555 1201",
    checkIn: addDays(today, 3),
    checkOut: addDays(today, 6),
    guests: 3,
    total: 255000,
    currency: "ARS",
    status: "CONFIRMED",
    paymentStatus: "PENDING",
    additional: "Desayuno × 3",
  },
  {
    id: "bk_2",
    guestName: "Lucas Fernández",
    guestEmail: "lucas@example.com",
    guestPhone: "+54 9 11 5555 1202",
    checkIn: addDays(today, 5),
    checkOut: addDays(today, 9),
    guests: 2,
    total: 340,
    currency: "USD",
    status: "PENDING",
    paymentStatus: "PENDING",
    additional: "Lancha 09:30hs",
  },
  {
    id: "bk_3",
    guestName: "Sofía Ramírez",
    guestEmail: "sofia@example.com",
    guestPhone: "+54 9 11 5555 1203",
    checkIn: addDays(today, 8),
    checkOut: addDays(today, 11),
    guests: 4,
    total: 255000,
    currency: "ARS",
    status: "CONFIRMED",
    paymentStatus: "PAID",
    additional: "Excursión al delta",
  },
  {
    id: "bk_4",
    guestName: "Diego Torres",
    guestEmail: "diego@example.com",
    guestPhone: "+54 9 11 5555 1204",
    checkIn: addDays(today, 12),
    checkOut: addDays(today, 14),
    guests: 2,
    total: 170000,
    currency: "ARS",
    status: "PENDING",
    paymentStatus: "PENDING",
    additional: "",
  },
  {
    id: "bk_5",
    guestName: "Camila Suárez",
    guestEmail: "camila@example.com",
    guestPhone: "+54 9 11 5555 1205",
    checkIn: addDays(today, -10),
    checkOut: addDays(today, -7),
    guests: 2,
    total: 255000,
    currency: "ARS",
    status: "COMPLETED",
    paymentStatus: "PAID",
    additional: "Desayuno × 2",
  },
];

const seedBlockedDays = [addDays(today, 4), addDays(today, 15)];

// Límite estricto de métodos de pago configurables (3 a 4 métodos)
export const MAX_PAYMENT_METHODS = 4;

const seedPayments = {
  primary: "ARS",
  wallets: ["USD", "EUR"],
  methods: [
    {
      id: "pm_1",
      type: "WALLET",
      label: "Mercado Pago",
      enabled: true,
      keys: {
        alias: "hostflow.mp",
        cvu: "0000003100012345678901",
        publicKey: "APP_USR-839210-9182",
        accessToken: "TEST-48291048291-039120",
      },
      notes: "Transferir al CVU o Alias y enviar comprobante por WhatsApp.",
    },
    {
      id: "pm_2",
      type: "BANK",
      label: "Transferencia Bancaria",
      enabled: true,
      keys: {
        holder: "Cabañas del Arroyo SRL",
        cbu: "0720000188000038291024",
        alias: "cabanas.arroyo",
        bank: "Banco Santander",
      },
      notes: "Enviar comprobante indicando el número de reserva.",
    },
    {
      id: "pm_3",
      type: "WALLET",
      label: "PayPal",
      enabled: true,
      keys: {
        email: "pagos@hostflow.app",
        clientId: "AYz_demo_clientId_9910",
        paypalMe: "paypal.me/hostflowdemo",
      },
      notes: "Aceptamos pagos internacionales en USD o EUR.",
    },
    {
      id: "pm_4",
      type: "CARD",
      label: "Tarjeta de crédito",
      enabled: false,
      keys: {
        publishableKey: "pk_test_51Oz...demo92",
        secretKey: "sk_test_...demo41",
      },
      notes: "Procesamiento de pagos con tarjeta de crédito o débito.",
    },
  ],
  // Datos bancarios que el alojamiento muestra al huésped para pagar por
  // transferencia externa (la plataforma no procesa pagos electrónicos).
  // El dueño los completa desde /panel/pagos; vacíos hasta que se carguen.
  bankDetails: {
    holder: "Cabañas del Arroyo SRL",
    alias: "cabanas.arroyo",
    cbu: "0720000188000038291024",
    notes: "Envíanos el comprobante por WhatsApp.",
  },
};

// Servicios adicionales que el alojamiento ofrece a sus huéspedes.
// `pricing`: PER_NIGHT = se cobra por noche; FIXED = precio fijo por unidad
// (el huésped elige la cantidad). `price` es siempre el precio unitario.
// `perGuest`: además se multiplica por la cantidad de huéspedes (comidas).
const seedServices = [
  { id: "sv_1", name: "Desayuno casero", price: 3500, pricing: "PER_NIGHT", perGuest: true, description: "Café, medialunas y fruta por persona y por noche.", enabled: true },
  { id: "sv_2", name: "Excursión al delta", price: 18000, pricing: "FIXED", perGuest: true, description: "Media jornada guiada en lancha.", enabled: true },
  { id: "sv_3", name: "Cuna", price: 0, pricing: "FIXED", description: "Sujeta a disponibilidad.", enabled: false },
  { id: "sv_4", name: "Limpieza extra", price: 8000, pricing: "FIXED", description: "Limpieza profunda adicional durante la estadía.", enabled: true },
];

const seedAdmin = {
  lodgings: [
    { id: "lod_1", name: "Cabañas del Arroyo", owner: "Martina Silva", ownerEmail: "anfitrion@hostflow.app", rooms: 4, status: "ACTIVE" },
    { id: "lod_2", name: "Posada del Delta", owner: "Juan Pérez", ownerEmail: "juan@posada.com", rooms: 6, status: "ACTIVE" },
    { id: "lod_3", name: "Hotel Río Luján", owner: "Laura Gómez", ownerEmail: "laura@rulujan.com", rooms: 12, status: "ACTIVE" },
    { id: "lod_4", name: "Cabañas La Lancha", owner: "Pedro Díaz", ownerEmail: "pedro@lalancha.com", rooms: 3, status: "PAUSED" },
    { id: "lod_5", name: "Albergue Tigre", owner: "Ana López", ownerEmail: "ana@albergue.com", rooms: 8, status: "ACTIVE" },
    { id: "lod_6", name: "Lo del Mono", owner: "Rosa Castro", ownerEmail: "rosa@lodemono.com", rooms: 2, status: "ACTIVE" },
    { id: "lod_7", name: "Mirador del Paraná", owner: "Nico Ruiz", ownerEmail: "nico@mirador.com", rooms: 5, status: "PAUSED" },
    { id: "lod_8", name: "Casa Flotante", owner: "Elena Vidal", ownerEmail: "elena@casaf.com", rooms: 1, status: "ACTIVE" },
  ],
};

function buildInitialState() {
  return {
    user: null,
    // Usuarios conocidos (se persisten para que los cambios de clave sobrevivan recargas).
    users: seedUsers.map((user) => ({ ...user })),
    lodging: {
      ...seedLodging,
      brand: { ...BRAND_DEFAULTS, ...seedLodging.brand },
      brandPalette: [],
    },
    // Cada reserva emite su enlace de llegada con vigencia de 24 h.
    bookings: seedBookings.map((b) => ({ ...b, onboardingLinkIssuedAt: onboardingLinkIssuedAt() })),
    blockedDays: [...seedBlockedDays],
    services: seedServices.map((s) => ({ ...s })),
    payments: {
      primary: seedPayments.primary,
      wallets: [...seedPayments.wallets],
      methods: seedPayments.methods.map((m) => ({ ...m })),
      bankDetails: { ...seedPayments.bankDetails },
    },
    admin: { lodgings: seedAdmin.lodgings.map((l) => ({ ...l })) },
    pendingOtp: null, // { email, code, expiresAt, password }
    lastOtp: null, // código mostrado en modo demo
  };
}

// Aplica migraciones/validaciones a un estado guardado por una versión previa
// de la demo. Se usa al cargar y al recibir el estado de OTRA pestaña.
function normalizeStored(stored, fallback) {
  const merged = { ...fallback, ...stored }
  // Compatibilidad con estados guardados por versiones anteriores.
  merged.lodging = {
    ...seedLodging,
    ...merged.lodging,
    brand: sanitizeBrand(merged.lodging?.brand),
    brandPalette: Array.isArray(merged.lodging?.brandPalette) ? merged.lodging.brandPalette : [],
    // El límite bajó a 4 fotos: los estados viejos con más se recortan.
    images: Array.isArray(merged.lodging?.images) ? merged.lodging.images.slice(0, MAX_IMAGES) : [],
  }
  merged.payments = {
    ...merged.payments,
    bankDetails: { ...seedPayments.bankDetails, ...merged.payments?.bankDetails },
    methods: Array.isArray(merged.payments?.methods)
      ? merged.payments.methods.slice(0, MAX_PAYMENT_METHODS).map((m) => {
          const seed = seedPayments.methods.find((s) => s.id === m.id);
          return {
            ...m,
            // Estados viejos no guardaban `enabled`: sin este valor la alerta
            // de "falta método de pago" aparecía aunque hubiera métodos.
            enabled: m.enabled ?? seed?.enabled ?? true,
            type: m.type || seed?.type || "WALLET",
            keys: { ...(seed?.keys || {}), ...(m.keys || {}) },
            notes: m.notes ?? seed?.notes ?? "",
          };
        })
      : seedPayments.methods.map((m) => ({ ...m })),
  }
  if (!Array.isArray(merged.services)) merged.services = seedServices.map((s) => ({ ...s }))
  // Servicios guardados por versiones anteriores: sin forma de cobro → precio fijo.
  // `perGuest` no existía antes: los servicios de ejemplo toman su valor del seed.
  merged.services = merged.services.map((sv) => ({
    ...sv,
    pricing: sv.pricing === "PER_NIGHT" ? "PER_NIGHT" : "FIXED",
    perGuest: sv.perGuest ?? seedServices.find((seed) => seed.id === sv.id)?.perGuest ?? false,
  }))
  // Reservas sin marca de vigencia (estados viejos): se emiten ahora y quedan persistidas.
  // Además, las reservas de ejemplo guardadas antes de sumar el teléfono lo toman
  // del seed: así el onboarding precarga los cuatro datos también en la demo.
  if (Array.isArray(merged.bookings)) {
    merged.bookings = merged.bookings.map((b) => {
      const seed = seedBookings.find((s) => s.id === b.id);
      return {
        ...b,
        guestPhone: b.guestPhone || seed?.guestPhone || "",
        onboardingLinkIssuedAt: b.onboardingLinkIssuedAt || onboardingLinkIssuedAt(),
        // Alergias: antes se guardaban como texto; ahora la respuesta es un
        // array (multiselección + "Otro"). Se migra sin perder el contenido.
        onboarding: b.onboarding
          ? {
              ...b.onboarding,
              allergies: Array.isArray(b.onboarding.allergies)
                ? b.onboarding.allergies.filter(Boolean)
                : String(b.onboarding.allergies || "")
                    .split(",")
                    .map((item) => item.trim())
                    .filter(Boolean),
            }
          : b.onboarding,
      };
    })
  }
  return merged
}

function loadState() {
  try {
    let raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      // Migración desde la versión anterior (sessionStorage por pestaña).
      const legacy = sessionStorage.getItem(STORAGE_KEY);
      if (legacy) {
        localStorage.setItem(STORAGE_KEY, legacy);
        raw = legacy;
      }
    }
    if (raw) return normalizeStored(JSON.parse(raw), buildInitialState());
  } catch {
    /* almacenamiento no disponible */
  }
  return buildInitialState();
}

// ── Núcleo pub/sub ──────────────────────────────────────────────────────────

let state = loadState();
const listeners = new Set();

function persist() {
  try {
    const { pendingOtp, lastOtp, ...persistable } = state;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(persistable));
  } catch {
    /* noop */
  }
}

// Otra pestaña/ventana de ESTE navegador cambió el estado → lo adoptamos para
// que el enlace de llegada abierto en paralelo vea los cambios del anfitrión
// (pago aceptado, nuevas reservas, marca, etc.) sin recargar.
try {
  window.addEventListener("storage", (event) => {
    if (event.key !== STORAGE_KEY || !event.newValue) return;
    try {
      // normalizeStored sin re-persistir: evita ciclos entre pestañas.
      state = normalizeStored(JSON.parse(event.newValue), buildInitialState());
      listeners.forEach((listener) => listener(state));
    } catch {
      /* JSON inválido: ignorar */
    }
  });
} catch {
  /* entorno sin window (SSR/tests) */
}

function emit() {
  persist();
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

export const store = {
  get: () => state,
  subscribe,
};

// ── Acciones (demo) ─────────────────────────────────────────────────────────

const delay = (ms = 320) => new Promise((resolve) => setTimeout(resolve, ms));

const publicUser = ({ password, ...user }) => user;

// Construye el objeto JSON de una solicitud de cotización de transporte en lancha.
// Estructura PROVISIONAL documentada en docs/boat-quote-contract.md: cuando la API
// destino confirme su contrato, solo hay que ajustar este mapeo.
function buildBoatQuotePayload(booking, quote) {
  return {
    schema: "hostflow.boat-quote.v1",
    createdAt: new Date().toISOString(),
    lodging: {
      id: state.lodging.id,
      name: state.lodging.name,
      location: state.lodging.location,
      phone: state.lodging.phone,
    },
    booking: booking
      ? {
          id: booking.id,
          checkIn: booking.checkIn,
          checkOut: booking.checkOut,
          guests: booking.guests,
        }
      : null,
    guest: {
      name: quote.guestName || booking?.guestName || "",
      email: quote.guestEmail || booking?.guestEmail || "",
      phone: quote.guestPhone || "",
    },
    trip: {
      service: "lancha_remis",
      pickupPlace: quote.pickupPlace || "",
      pickupDate: quote.pickupDate || booking?.checkIn || "",
      pickupTime: quote.pickupTime || "",
      dropoffPlace: quote.dropoffPlace || state.lodging.location || "",
      passengers: Number(quote.passengers) || 1,
      baggage: Number(quote.baggage) || 0,
      notes: quote.notes || "",
    },
  };
}

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
    const payload = buildBoatQuotePayload(booking, quote);
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

// Estado de aplicación en memoria (modo demo).
//
// Es la fuente de verdad del frontend cuando `VITE_API_URL` no está definida.
// Expone el MISMO contrato que el backend (ver docs/swagger-api.md): cuando se
// conecte el backend real, `src/lib/api/index.js` pasará a usar `httpApi` y este
// archivo se usa solo como referencia de datos de ejemplo.
//
// Persistencia: sessionStorage (se borra al cerrar la pestaña, como un token real).

import { addDays, toISODate } from "@utils/format";

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
  pricePerNight: 85000,
  currency: "ARS",
  images: [],
  logo: null,
  brand: { primary: "#0fa3a3", accent: "#d4f53c", radius: "rounded" },
};

const seedBookings = [
  {
    id: "bk_1",
    guestName: "María González",
    guestEmail: "maria@example.com",
    checkIn: addDays(today, 3),
    checkOut: addDays(today, 6),
    guests: 3,
    total: 255000,
    currency: "ARS",
    status: "CONFIRMED",
    additional: "Desayuno × 3",
  },
  {
    id: "bk_2",
    guestName: "Lucas Fernández",
    guestEmail: "lucas@example.com",
    checkIn: addDays(today, 5),
    checkOut: addDays(today, 9),
    guests: 2,
    total: 340,
    currency: "USD",
    status: "PENDING",
    additional: "Lancha 09:30hs",
  },
  {
    id: "bk_3",
    guestName: "Sofía Ramírez",
    guestEmail: "sofia@example.com",
    checkIn: addDays(today, 8),
    checkOut: addDays(today, 11),
    guests: 4,
    total: 255000,
    currency: "ARS",
    status: "CONFIRMED",
    additional: "Excursión al delta",
  },
  {
    id: "bk_4",
    guestName: "Diego Torres",
    guestEmail: "diego@example.com",
    checkIn: addDays(today, 12),
    checkOut: addDays(today, 14),
    guests: 2,
    total: 170000,
    currency: "ARS",
    status: "PENDING",
    additional: "",
  },
  {
    id: "bk_5",
    guestName: "Camila Suárez",
    guestEmail: "camila@example.com",
    checkIn: addDays(today, -10),
    checkOut: addDays(today, -7),
    guests: 2,
    total: 255000,
    currency: "ARS",
    status: "COMPLETED",
    additional: "Desayuno × 2",
  },
];

const seedBlockedDays = [addDays(today, 4), addDays(today, 15)];

const seedPayments = {
  primary: "ARS",
  wallets: ["USD", "EUR"],
  methods: [
    { id: "pm_1", type: "WALLET", label: "Mercado Pago", enabled: true },
    { id: "pm_2", type: "WALLET", label: "PayPal", enabled: true },
    { id: "pm_3", type: "CARD", label: "Tarjeta de crédito", enabled: true },
    { id: "pm_4", type: "WALLET", label: "Ualá", enabled: false },
  ],
};

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
    lodging: { ...seedLodging },
    bookings: seedBookings.map((b) => ({ ...b })),
    blockedDays: [...seedBlockedDays],
    payments: {
      primary: seedPayments.primary,
      wallets: [...seedPayments.wallets],
      methods: seedPayments.methods.map((m) => ({ ...m })),
    },
    admin: { lodgings: seedAdmin.lodgings.map((l) => ({ ...l })) },
    pendingOtp: null, // { email, code, expiresAt, password }
    lastOtp: null, // código mostrado en modo demo
  };
}

function loadState() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (raw) return { ...buildInitialState(), ...JSON.parse(raw) };
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
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(persistable));
  } catch {
    /* noop */
  }
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
  updateLodging(patch) {
    setState((s) => ({ ...s, lodging: { ...s.lodging, ...patch } }));
  },

  setLogo(dataUrl) {
    actions.updateLodging({ logo: dataUrl });
  },

  addImage(dataUrl) {
    setState((s) => ({ ...s, lodging: { ...s.lodging, images: [...s.lodging.images, dataUrl] } }));
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

  // ── Pagos ────────────────────────────────────────────────────────────────
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

  addPaymentMethod(label, type = "WALLET") {
    setState((s) => ({
      ...s,
      payments: {
        ...s.payments,
        methods: [...s.payments.methods, { id: `pm_${Date.now().toString(36)}`, type, label, enabled: true }],
      },
    }));
  },

  // ── Admin ────────────────────────────────────────────────────────────────
  setAdminLodgingStatus(id, status) {
    setState((s) => ({
      ...s,
      admin: {
        lodgings: s.admin.lodgings.map((l) => (l.id === id ? { ...l, status } : l)),
      },
    }));
  },
};

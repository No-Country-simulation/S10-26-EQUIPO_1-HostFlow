// Datos de ejemplo del modo demostración y estado inicial del store.
//
// Separado de store.js para que el núcleo de estado y las acciones no carguen
// con cientos de líneas de datos. En producción este archivo no se usa: las
// lecturas y escrituras pasan por la API real.
import { addDays, toISODate } from "@utils/format";
import { BRAND_DEFAULTS } from "@lib/domain/lodging";
import { onboardingLinkIssuedAt } from "@lib/domain/onboarding";

const today = toISODate();

export const seedUsers = [
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
  {
    id: "u_jesus",
    name: "Jesús",
    email: "jesus@hostflow.com",
    password: "jesus1234",
    role: "HOST",
    lodgingId: "lod_1",
  },
];

export const seedLodging = {
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

export const seedBookings = [
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

export const seedPayments = {
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
export const seedServices = [
  { id: "sv_1", name: "Desayuno casero", price: 3500, pricing: "PER_NIGHT", perGuest: true, description: "Café, medialunas y fruta por persona y por noche.", enabled: true },
  { id: "sv_2", name: "Excursión al delta", price: 18000, pricing: "FIXED", perGuest: true, description: "Media jornada guiada en lancha.", enabled: true },
  { id: "sv_3", name: "Cuna", price: 0, pricing: "FIXED", description: "Sujeta a disponibilidad.", enabled: false },
  { id: "sv_4", name: "Limpieza extra", price: 8000, pricing: "FIXED", description: "Limpieza profunda adicional durante la estadía.", enabled: true },
];

export const seedAdmin = {
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

export function buildInitialState() {
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

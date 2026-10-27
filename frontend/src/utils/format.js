// Utilidades de formato y validación compartidas.

export const cn = (...items) => items.filter(Boolean).join(" ");

export const isEmail = (value = "") => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value.trim());

// ── Monedas ────────────────────────────────────────────────────────────────

export const CURRENCIES = {
  ARS: { code: "ARS", symbol: "$", label: "Pesos Argentinos", locale: "es-AR" },
  USD: { code: "USD", symbol: "US$", label: "Dólares", locale: "en-US" },
  EUR: { code: "EUR", symbol: "€", label: "Euros", locale: "es-ES" },
};

export const WALLETS = ["Mercado Pago", "PayPal", "Ualá", "Binance Pay", "AirTM"];

export function formatMoney(amount = 0, currency = "ARS") {
  const config = CURRENCIES[currency] || CURRENCIES.ARS;
  try {
    return new Intl.NumberFormat(config.locale, {
      style: "currency",
      currency,
      maximumFractionDigits: currency === "ARS" ? 0 : 2,
    }).format(amount);
  } catch {
    return `${config.symbol} ${amount.toLocaleString("es-AR")}`;
  }
}

export const currencySymbol = (currency = "ARS") => CURRENCIES[currency]?.symbol || "$";

// ── Fechas ─────────────────────────────────────────────────────────────────

const LOCALE = "es-AR";

// Interpreta "YYYY-MM-DD" como fecha LOCAL y no como UTC.
// `new Date("2026-10-24")` se parsea como medianoche UTC, que en Argentina
// (UTC-3) es el 23 a las 21:00 y desplaza todas las fechas un día hacia atrás.
export function parseISODate(iso) {
  if (!iso) return null
  const [y, m, d] = String(iso).slice(0, 10).split("-").map(Number)
  if (!y || !m || !d) return null
  return new Date(y, m - 1, d)
}

export function formatDate(iso) {
  const date = parseISODate(iso)
  if (!date) return "—"
  return new Intl.DateTimeFormat(LOCALE, { day: "numeric", month: "short", year: "numeric" }).format(date)
}

export function formatDateShort(iso) {
  const date = parseISODate(iso)
  if (!date) return "—"
  return new Intl.DateTimeFormat(LOCALE, { day: "2-digit", month: "short" }).format(date)
}

export function toISODate(date = new Date()) {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${day}`
}

export function addDays(iso, days) {
  const date = parseISODate(iso)
  if (!date) return iso
  date.setDate(date.getDate() + days)
  return toISODate(date)
}

export function daysBetween(fromISO, toISO) {
  const from = parseISODate(fromISO)
  const to = parseISODate(toISO)
  if (!from || !to) return 1
  return Math.max(1, Math.round((to - from) / 86_400_000))
}

export function weekdayShort(iso) {
  const date = parseISODate(iso)
  if (!date) return "—"
  return new Intl.DateTimeFormat(LOCALE, { weekday: "short" }).format(date)
}

export function isPast(iso) {
  const date = parseISODate(iso)
  if (!date) return false
  return date < parseISODate(toISODate())
}

// ── Texto ─────────────────────────────────────────────────────────────────

export function initials(name = "") {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

export function plural(count, singular, pluralForm) {
  return `${count} ${count === 1 ? singular : pluralForm}`;
}

export function maskEmail(email = "") {
  const [user, domain] = email.split("@");
  if (!user || !domain) return email;
  const head = user.slice(0, 2);
  return `${head}${"•".repeat(Math.max(1, user.length - 2))}@${domain}`;
}

// ── Totales de una reserva ────────────────────────────────────────────────
// El campo `total` de la reserva es SOLO el alojamiento. El total real (lo
// que el huésped paga) incluye los servicios adicionales que registró en el
// onboarding (booking.selectedServices): el importe debe reflejarse siempre
// en el total y en su detallado.

export function bookingServices(booking) {
  return Array.isArray(booking?.selectedServices) ? booking.selectedServices : [];
}

export function bookingServicesTotal(booking) {
  return bookingServices(booking).reduce((sum, item) => sum + (Number(item.price) || 0), 0);
}

export function bookingGrandTotal(booking) {
  return (Number(booking?.total) || 0) + bookingServicesTotal(booking);
}

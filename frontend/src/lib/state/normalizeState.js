// Migraciones y validaciones del estado persistido en localStorage.
//
// Se aplica al cargar la demo y al recibir el estado de otra pestaña. Vive fuera
// de store.js para que el núcleo de estado quede chico y este archivo pueda
// evolucionar sin tocar las acciones.
import { MAX_IMAGES, sanitizeBrand } from "@lib/domain/lodging";
import { MAX_PAYMENT_METHODS } from "@lib/domain/payments";
import { onboardingLinkIssuedAt } from "@lib/domain/onboarding";
import { seedBookings, seedLodging, seedPayments, seedServices } from "./seedData";

// Aplica migraciones/validaciones a un estado guardado por una versión previa
// de la demo. Se usa al cargar y al recibir el estado de OTRA pestaña.
export function normalizeStored(stored, fallback) {
  const merged = { ...fallback, ...stored };
  // Compatibilidad con estados guardados por versiones anteriores.
  merged.lodging = {
    ...seedLodging,
    ...merged.lodging,
    brand: sanitizeBrand(merged.lodging?.brand),
    brandPalette: Array.isArray(merged.lodging?.brandPalette) ? merged.lodging.brandPalette : [],
    // El límite bajó a 4 fotos: los estados viejos con más se recortan.
    images: Array.isArray(merged.lodging?.images) ? merged.lodging.images.slice(0, MAX_IMAGES) : [],
  };
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
  };
  if (!Array.isArray(merged.services)) merged.services = seedServices.map((s) => ({ ...s }));
  // Servicios guardados por versiones anteriores: sin forma de cobro → precio fijo.
  // `perGuest` no existía antes: los servicios de ejemplo toman su valor del seed.
  merged.services = merged.services.map((sv) => ({
    ...sv,
    pricing: sv.pricing === "PER_NIGHT" ? "PER_NIGHT" : "FIXED",
    perGuest: sv.perGuest ?? seedServices.find((seed) => seed.id === sv.id)?.perGuest ?? false,
  }));
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
    });
  }
  return merged;
}

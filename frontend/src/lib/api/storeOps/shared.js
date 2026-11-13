// Helpers de normalización que LEEN el estado actual del store.
// No escriben nada: las escrituras viven en cada operación (storeOps/*).
import { store } from "../../state/store"
import { MAX_IMAGES } from "../../domain/lodging"

// Backend → estado interno del alojamiento. Se acepta el shape documentado y
// se conserva el valor local cuando el backend no manda el campo.
export function mergeLodging(dto) {
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
export function toBooking(dto) {
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

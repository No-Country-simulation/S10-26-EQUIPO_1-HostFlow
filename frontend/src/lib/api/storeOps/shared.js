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
    id: dto.id ? String(dto.id) : current.id,
    name: dto.name || dto.accommodationName || current.name,
    tagline: dto.welcomeMessage ?? dto.tagline ?? current.tagline,
    location: dto.address ?? dto.location ?? current.location,
    phone: dto.phone ?? current.phone,
    rooms: Number(dto.roomCount ?? dto.rooms ?? current.rooms),
    bathrooms: Number(dto.bathroomCount ?? dto.bathrooms ?? current.bathrooms),
    capacity: Number(dto.guestCapacity ?? dto.capacity ?? current.capacity),
    pricePerNight: Number(dto.basePrice ?? dto.pricePerNight ?? current.pricePerNight),
    currency: dto.currency || current.currency,
    email: dto.email ?? current.email ?? "",
    images: Array.isArray(dto.images)
      ? dto.images.map((img) => (typeof img === "string" ? img : img.url ?? img.imageUrl)).slice(0, MAX_IMAGES)
      : current.images,
    galleryImages: Array.isArray(dto.images) && typeof dto.images[0] === "object" ? dto.images : current.galleryImages || [],
    logo: dto.logoUrl ?? dto.logo ?? current.logo,
    brand: {
      ...current.brand,
      ...(dto.primaryColor ? { primary: dto.primaryColor } : {}),
      ...(dto.accentColor ? { accent: dto.accentColor } : {}),
      ...(dto.tertiaryColor ? { tertiary: dto.tertiaryColor } : {}),
      ...(dto.backgroundPattern ? { pattern: dto.backgroundPattern } : {}),
      ...(dto.brand || {}),
    },
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
    id: String(dto.id),
    rawId: dto.id,
    guestName: dto.guestFullName || dto.guestName || current?.guestName || "Huésped",
    guestEmail: dto.guestEmail || current?.guestEmail || "",
    guestPhone: dto.guestPhone || current?.guestPhone || "",
    checkIn: dto.checkIn ? String(dto.checkIn).slice(0, 10) : (current?.checkIn || ""),
    checkOut: dto.checkOut ? String(dto.checkOut).slice(0, 10) : (current?.checkOut || ""),
    guests: Number(dto.guestCount ?? dto.guests ?? current?.guests ?? 1),
    total: Number(dto.totalAmount ?? dto.total ?? current?.total ?? 0),
    currency: dto.currency || current?.currency || store.get().lodging.currency,
    status: dto.status || current?.status || "PENDING",
    paymentStatus: dto.paymentStatus || current?.paymentStatus || "PENDING",
    additional: dto.specialRequests ?? dto.additional ?? current?.additional ?? "",
    selectedServices: Array.isArray(dto.selectedServices)
      ? dto.selectedServices
      : current?.selectedServices || [],
    onboardingLinkIssuedAt:
      dto.confirmationLink?.createdAt || dto.onboardingLinkIssuedAt || current?.onboardingLinkIssuedAt,
    onboarding: dto.onboarding || current?.onboarding,
    boatQuote: dto.boatQuote || current?.boatQuote,
  }
}

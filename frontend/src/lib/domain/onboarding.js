// Reglas del enlace de llegada del huésped (onboarding B2C).

// Vigencia del enlace de llegada (spec: dura 24 h desde su emisión).
export const ONBOARDING_LINK_TTL_MS = 24 * 60 * 60 * 1000;

export const onboardingLinkIssuedAt = () => new Date().toISOString();

export function isOnboardingLinkExpired(booking) {
  const issued = booking?.onboardingLinkIssuedAt ? Date.parse(booking.onboardingLinkIssuedAt) : NaN;
  if (Number.isNaN(issued)) return false; // reservas antiguas sin marca: sin expiración
  return Date.now() - issued > ONBOARDING_LINK_TTL_MS;
}

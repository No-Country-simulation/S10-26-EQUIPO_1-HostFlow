// Reglas de marca y galería del alojamiento.
//
// Viven fuera del store para que las vistas, los adaptadores de API y el estado
// compartan las mismas constantes sin acoplarse a la implementación de la demo.
import { normalizeHex } from "@utils/color";
import { DEFAULT_PATTERN, PATTERN_IDS } from "@lib/theme/patterns";

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
export function sanitizeBrand(brand) {
  return {
    primary: normalizeHex(brand?.primary, BRAND_DEFAULTS.primary),
    accent: normalizeHex(brand?.accent, BRAND_DEFAULTS.accent),
    tertiary: normalizeHex(brand?.tertiary, BRAND_DEFAULTS.tertiary),
    radius: BRAND_RADIUS.includes(brand?.radius) ? brand.radius : BRAND_DEFAULTS.radius,
    pattern: PATTERN_IDS.includes(brand?.pattern) ? brand.pattern : BRAND_DEFAULTS.pattern,
  };
}

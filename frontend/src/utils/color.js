// Utilidades de color compartidas (marca del alojamiento y extracción de paletas).
// Todas las funciones trabajan con hex "#rrggbb" y son puras: sin DOM.

export function hexToRgb(hex) {
  const value = String(hex || "").replace("#", "");
  if (value.length !== 3 && value.length !== 6) return null;
  const full = value.length === 3 ? value.split("").map((c) => c + c).join("") : value;
  const r = parseInt(full.slice(0, 2), 16);
  const g = parseInt(full.slice(2, 4), 16);
  const b = parseInt(full.slice(4, 6), 16);
  if ([r, g, b].some(Number.isNaN)) return null;
  return { r, g, b };
}

export function rgbToHex(r, g, b) {
  const channel = (v) => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, "0");
  return `#${channel(r)}${channel(g)}${channel(b)}`;
}

export function normalizeHex(value, fallback = "#0fa3a3") {
  return hexToRgb(value) ? `#${String(value).replace("#", "").toLowerCase()}` : fallback;
}

// Luminancia percibida (ITU-R BT.709), 0..1.
export function relativeLuminance({ r, g, b }) {
  return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
}

// ¿El color es claro? Define si sobre él va tinta (#111) o blanco.
export function isLightColor(hex, threshold = 0.55) {
  const rgb = hexToRgb(hex);
  if (!rgb) return true;
  return relativeLuminance(rgb) > threshold;
}

// Texto legible sobre el color dado (mismo criterio que la paleta base:
// tinta sobre lima, blanco sobre teal).
// Versión WCAG: elige el color de texto con MEJOR ratio real sobre el fondo.
// Cualquier color da ≥ 4.58:1 con la mejor de las dos tintas (punto de igualdad
// entre blanco y negro), así el texto siempre cumple AA.
export function contrastRatio(a, b) {
  const lum = (hex) => {
    const rgb = hexToRgb(hex);
    if (!rgb) return 0;
    const lin = (v) => {
      const c = v / 255;
      return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    };
    return 0.2126 * lin(rgb.r) + 0.7152 * lin(rgb.g) + 0.0722 * lin(rgb.b);
  };
  const la = lum(a);
  const lb = lum(b);
  const [hi, lo] = la > lb ? [la, lb] : [lb, la];
  return (hi + 0.05) / (lo + 0.05);
}

export const readableOn = (hex) => {
  if (!hexToRgb(hex)) return "#111111";
  return contrastRatio(hex, "#ffffff") >= contrastRatio(hex, "#111111") ? "#ffffff" : "#111111";
};

export function darken(hex, amount = 0.15) {
  const rgb = hexToRgb(hex);
  if (!rgb) return hex;
  return rgbToHex(
    Math.round(rgb.r * (1 - amount)),
    Math.round(rgb.g * (1 - amount)),
    Math.round(rgb.b * (1 - amount)),
  );
}

export function lighten(hex, amount = 0.15) {
  const rgb = hexToRgb(hex);
  if (!rgb) return hex;
  const up = (v) => Math.round(v + (255 - v) * amount);
  return rgbToHex(up(rgb.r), up(rgb.g), up(rgb.b));
}

// Variante "strong" de un color SIEMPRE legible sobre el fondo indicado:
// oscurece (tema claro) o aclara (tema oscuro) hasta lograr ratio ≥ 4.5:1.
export function strongOn(hex, bg, { dark = false } = {}) {
  let out = hexToRgb(hex) ? hex : "#0fa3a3";
  for (let i = 0; i < 14 && contrastRatio(out, bg) < 4.5; i += 1) {
    out = dark ? lighten(out, 0.14) : darken(out, 0.14);
  }
  return out;
}

// Color VISIBLE sobre un fondo (no texto, sino superficies/bandas/patrones):
// si el fondo es oscuro aclara el color, si es claro lo oscurece, hasta que se
// distinga del fondo. Evita que una marca oscura "desaparezca" en modo oscuro.
export function visibleOn(hex, bg, min = 2.2) {
  const bgRgb = hexToRgb(bg);
  const dark = bgRgb ? relativeLuminance(bgRgb) < 0.5 : false;
  let out = hexToRgb(hex) ? hex : "#0fa3a3";
  for (let i = 0; i < 12 && contrastRatio(out, bg) < min; i += 1) {
    out = dark ? lighten(out, 0.12) : darken(out, 0.12);
  }
  return out;
}

// Distancia euclídea entre dos colores (para descartar parecidos en paletas).
export function colorDistance(a, b) {
  const dr = a.r - b.r;
  const dg = a.g - b.g;
  const db = a.b - b.b;
  return Math.sqrt(dr * dr + dg * dg + db * db);
}

// Saturación 0..1 de un color RGB.
export function saturation({ r, g, b }) {
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  return max === 0 ? 0 : (max - min) / max;
}

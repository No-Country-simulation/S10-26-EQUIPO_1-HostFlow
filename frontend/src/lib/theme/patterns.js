// Patrones de fondo de la marca del alojamiento.
//
// Cada patrón es una MÁSCARA (CSS mask) con formas repetidas: el color lo pone
// el tema con `--hf-pattern-color`, así el dibujo (hojas, olas, montañas…) toma
// el color de marca sin necesitar un archivo por color. Sin imágenes externas.
//
// El set está pensado para hoteles y cabañas: motivos naturales y de línea
// fina, con opacidades distintas para dar profundidad sin recargar. No hay
// puntos, grillas ni rayas: texturas con carácter, no relleno genérico.
//
// Este módulo es la única fuente de verdad: lo usan el tema (LodgingTheme) y el
// editor del panel.

// SVG inline → data URI (se usa como mask-image: las formas son opacas).
const svgMask = (body, width, height) =>
  `url("data:image/svg+xml,${encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 ${width} ${height}'>${body}</svg>`,
  )}")`;

// Hojas: una hoja grande y otra chica (con menos peso) por baldosa.
const leaves = svgMask(
  `<g fill='#000'><path d='M7 31C7 18 18 7 31 7c0 13-11 24-24 24Z'/><path d='M28 43c0-7 6-13 13-13 0 7-6 13-13 13Z' fill-opacity='.55'/></g>`,
  48,
  48,
);

// Olas: dos líneas onduladas desfasadas, la de atrás más suave.
const waves = svgMask(
  `<g fill='none' stroke='#000' stroke-width='2' stroke-linecap='round'><path d='M0 7c4-4 8-4 12 0s8 4 12 0 8-4 12 0 8 4 12 0'/><path d='M-12 19c4-4 8-4 12 0s8 4 12 0 8-4 12 0 8 4 12 0' stroke-opacity='.5'/></g>`,
  48,
  24,
);

// Pétalos, arcos, escamas y sol quedaron fuera: tres patrones alcanzan para
// dar identidad y elegir es más rápido. Todos comparten el mismo carácter
// natural (hojas, agua y montañas).

// Montañas: cordón de línea con sol (paisaje de cabaña).
const mountains = svgMask(
  `<g fill='none' stroke='#000' stroke-linecap='round' stroke-linejoin='round'><path d='M0 29 14 11l8 9L34 6l14 17 8-6' stroke-width='1.8'/><circle cx='45' cy='8' r='3.2' stroke-width='1.4' stroke-opacity='.6'/></g>`,
  56,
  32,
);

// [mask, size] de cada patrón. `size` escala la repetición.
const DEFS = {
  leaves: [leaves, "52px 52px"],
  waves: [waves, "48px 24px"],
  mountains: [mountains, "56px 32px"],
};

export const PATTERNS = [
  { id: "none", label: "Liso" },
  { id: "leaves", label: "Hojas" },
  { id: "waves", label: "Olas" },
  { id: "mountains", label: "Montañas" },
];

export const PATTERN_IDS = PATTERNS.map((pattern) => pattern.id);
export const DEFAULT_PATTERN = "none";

// Escala los valores en px de un `mask-size` (deja intacto "auto").
const scaleSize = (size, factor) =>
  size.replace(/([\d.]+)px/g, (_, px) => `${Math.round(Number(px) * factor * 10) / 10}px`);

// [mask-image, mask-size] listos para las variables CSS
// `--hf-pattern-mask` / `--hf-pattern-size` del scope de marca. `scale` agranda
// el motivo: en la página completa conviene más grande (menos repetitivo, más
// "textil") y en la miniatura del panel más chico para que se lea el motivo.
export function patternVars(id, scale = 1) {
  const [mask, size] = DEFS[id] || ["none", "auto"];
  return [mask, scale === 1 ? size : scaleSize(size, scale)];
}

// Estilo de la miniatura del panel: misma máscara pero más chica y con
// `currentColor` de fondo, así hereda el color de texto y funciona en claro y
// oscuro. Devuelve un objeto listo para `style`.
export function patternPreviewStyle(id) {
  const [mask, size] = patternVars(id, 0.62);
  if (mask === "none") return { backgroundColor: "transparent" };
  return {
    backgroundColor: "currentColor",
    maskImage: mask,
    maskSize: size,
    maskRepeat: "repeat",
    WebkitMaskImage: mask,
    WebkitMaskSize: size,
    WebkitMaskRepeat: "repeat",
  };
}

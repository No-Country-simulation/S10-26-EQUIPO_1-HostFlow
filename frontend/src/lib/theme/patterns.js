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

// Pétalos: flor de cinco pétalos dibujada con línea fina y centro lleno.
const petals = svgMask(
  `<g transform='translate(20 20)' fill='none' stroke='#000' stroke-width='1.5'><ellipse cx='0' cy='-8' rx='4.4' ry='7'/><ellipse cx='0' cy='-8' rx='4.4' ry='7' transform='rotate(72)'/><ellipse cx='0' cy='-8' rx='4.4' ry='7' transform='rotate(144)'/><ellipse cx='0' cy='-8' rx='4.4' ry='7' transform='rotate(216)'/><ellipse cx='0' cy='-8' rx='4.4' ry='7' transform='rotate(288)'/><circle r='2.2' fill='#000' stroke='none'/></g>`,
  40,
  40,
);

// Arcos: dos semicírculos concéntricos (aire boho/mediterráneo).
const arches = svgMask(
  `<g fill='none' stroke='#000' stroke-linecap='round'><path d='M4 27a18 18 0 0 1 36 0' stroke-width='2'/><path d='M11 27a11 11 0 0 1 22 0' stroke-width='1.5' stroke-opacity='.55'/></g>`,
  44,
  30,
);

// Montañas: cordón de línea con sol (paisaje de cabaña).
const mountains = svgMask(
  `<g fill='none' stroke='#000' stroke-linecap='round' stroke-linejoin='round'><path d='M0 29 14 11l8 9L34 6l14 17 8-6' stroke-width='1.8'/><circle cx='45' cy='8' r='3.2' stroke-width='1.4' stroke-opacity='.6'/></g>`,
  56,
  32,
);

// Escamas: arcos superpuestos tipo teja/escama de pez.
const scales = svgMask(
  `<g fill='none' stroke='#000' stroke-width='1.4'><path d='M0 23a16 16 0 0 1 32 0'/><path d='M-16 -1a16 16 0 0 1 32 0' stroke-opacity='.55'/><path d='M16 -1a16 16 0 0 1 32 0' stroke-opacity='.55'/></g>`,
  32,
  24,
);

// Sol: círculo con rayos, para estadías de día.
const sun = svgMask(
  `<g fill='none' stroke='#000' stroke-linecap='round'><circle cx='24' cy='24' r='6.5' stroke-width='1.8'/><g stroke-width='1.6'><path d='M24 5v4.5M24 38.5V43M5 24h4.5M38.5 24H43M10.6 10.6l3.2 3.2M34.2 34.2l3.2 3.2M37.4 10.6l-3.2 3.2M13.8 34.2l-3.2 3.2'/></g></g>`,
  48,
  48,
);

// [mask, size] de cada patrón. `size` escala la repetición.
const DEFS = {
  leaves: [leaves, "52px 52px"],
  waves: [waves, "48px 24px"],
  petals: [petals, "52px 52px"],
  arches: [arches, "44px 30px"],
  mountains: [mountains, "56px 32px"],
  scales: [scales, "34px 26px"],
  sun: [sun, "64px 64px"],
};

export const PATTERNS = [
  { id: "none", label: "Liso" },
  { id: "leaves", label: "Hojas" },
  { id: "waves", label: "Olas" },
  { id: "petals", label: "Pétalos" },
  { id: "arches", label: "Arcos" },
  { id: "mountains", label: "Montañas" },
  { id: "scales", label: "Escamas" },
  { id: "sun", label: "Sol" },
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

// Extracción de la paleta dominante de un logo (u otra imagen) en el navegador.
// Sirve para SUGERIR los colores personalizados del alojamiento a partir de su
// propio logo: no cambia nada hasta que el dueño elija un color.
//
// Cómo funciona: se dibuja la imagen en un canvas pequeño, se agrupan los píxeles
// en cubos cuantizados y se puntúan por frecuencia + saturación, descartando
// fondos, blancos, negros y grises casi neutros (no sirven como colores de marca).
// Sin dependencias nuevas ni peticiones de red: la imagen nunca sale del navegador.
import { colorDistance, hexToRgb, relativeLuminance, rgbToHex, saturation } from "@utils/color";

const WORK_SIZE = 128; // tamaño de trabajo: suficiente y rápido

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("No se pudo cargar la imagen del logo."));
    img.src = src;
  });
}

// Devuelve hasta `maxColors` hex ("#rrggbb") ordenados por score (frecuencia y
// saturación). Lanza Error con un mensaje apto para el usuario si no hay color.
export async function extractPaletteFromImage(src, { maxColors = 8 } = {}) {
  const img = await loadImage(src);

  const canvas = document.createElement("canvas");
  canvas.width = WORK_SIZE;
  canvas.height = WORK_SIZE;
  const ctx = canvas.getContext("2d", { willReadFrequently: true });
  // Escala a cubo: funciona igual con logos cuadrados, rectangulares o SVG.
  ctx.drawImage(img, 0, 0, WORK_SIZE, WORK_SIZE);

  let data;
  try {
    data = ctx.getImageData(0, 0, WORK_SIZE, WORK_SIZE).data;
  } catch {
    throw new Error("No se pudieron leer los píxeles del logo.");
  }

  const buckets = new Map();

  for (let i = 0; i < data.length; i += 4) {
    // Píxeles transparentes o semitransparentes: se ignoran.
    if (data[i + 3] < 200) continue;

    const r = data[i];
    const g = data[i + 1];
    const b = data[i + 2];
    const rgb = { r, g, b };
    const sat = saturation(rgb);
    const light = relativeLuminance(rgb);

    // Filtra casi-blancos, casi-negros y grises: no son colores de marca usables.
    if (light > 0.94 || light < 0.08) continue;
    if (sat < 0.12) continue;

    // Cubo de 32 niveles por canal (5 bits).
    const key = `${r >> 3},${g >> 3},${b >> 3}`;
    const bucket = buckets.get(key) || { r: 0, g: 0, b: 0, count: 0, sat: 0 };
    bucket.r += r;
    bucket.g += g;
    bucket.b += b;
    bucket.sat += sat;
    bucket.count += 1;
    buckets.set(key, bucket);
  }

  const candidates = [...buckets.values()]
    .map((bucket) => {
      const rgb = {
        r: Math.round(bucket.r / bucket.count),
        g: Math.round(bucket.g / bucket.count),
        b: Math.round(bucket.b / bucket.count),
      };
      const avgSat = bucket.sat / bucket.count;
      // Frecuencia ponderada por saturación: los colores vivos del logo pesan más
      // que el gris del texto, pero un gris muy dominante igual puede aparecer.
      const score = bucket.count * (0.4 + 0.6 * avgSat);
      return { hex: rgbToHex(rgb.r, rgb.g, rgb.b), rgb, score };
    })
    .sort((a, b) => b.score - a.score);

  // Deduplica colores parecidos (umbral de distancia) conservando el dominante.
  const picked = [];
  for (const candidate of candidates) {
    if (picked.some((p) => colorDistance(p.rgb, candidate.rgb) < 60)) continue;
    picked.push(candidate);
    if (picked.length >= maxColors) break;
  }

  if (picked.length === 0) {
    throw new Error("No se detectaron colores utilizables en el logo. Probá con uno con más color.");
  }

  return picked.map((p) => p.hex);
}

// Colores sugeridos para los 3 slots de marca a partir de una paleta extraída.
export function suggestBrandTrio(palette) {
  const [primary, accent, tertiary] = palette;
  return {
    ...(primary && { primary: normalizeOrKeep(primary) }),
    ...(accent && { accent: normalizeOrKeep(accent) }),
    ...(tertiary && { tertiary: normalizeOrKeep(tertiary) }),
  };
}

// Asegura el formato "#rrggbb" minúscula.
function normalizeOrKeep(hex) {
  const rgb = hexToRgb(hex);
  return rgb ? rgbToHex(rgb.r, rgb.g, rgb.b) : hex;
}

// Aplica la identidad visual (3 colores con rol, forma y patrón) de UN
// alojamiento sobre el sistema de temas existente.
//
// Roles de color (fijos, no los decide el usuario):
//   primary  → "Marca":  cabecera/portada del enlace, datos, íconos.
//   accent   → "Acción": botones, precio y selección.
//   tertiary → "Fondo":  superficie de la página y acentos suaves.
//
// No es un sistema de temas paralelo: redefine las MISMAS variables CSS que
// emite `@theme` en styles/globals.css (`--color-teal`, `--color-lime`, …),
// pero SOLO dentro del contenedor donde se monta. Así:
//   · el dashboard, el panel de administración y otros alojamientos no cambian;
//   · el modo claro/oscuro global sigue intacto (los neutros no se tocan);
//   · todos los componentes existentes (botones, badges, cards) adoptan la
//     marca del alojamiento automáticamente.
//
// Las variables van en un <style> con clase única (useId) y NO inline: las
// variables inline ganan siempre sobre la hoja de estilos global, por lo que
// una regla `.dark` del stylesheet no podría sobreescribirlas. Acá tanto las
// reglas base como las del modo oscuro viven en el mismo <style> scoped, y
// las variantes `*-strong` se recalculan por tema hasta cumplir ratio ≥ 4.5:1
// (WCAG AA) contra el fondo real (#fff en claro, #111 en oscuro).
//
// Uso: <LodgingTheme brand={lodging.brand}>…</LodgingTheme> (páginas del huésped
// y vistas previas del panel).
import { useId, useMemo } from "react";
import { normalizeHex, readableOn, strongOn, visibleOn } from "@utils/color";
import { cn } from "@utils/format";
import { DEFAULT_PATTERN, PATTERN_IDS, patternVars } from "./patterns";

// Forma fija y prolija: las esquinas ya no se configuran (el selector no
// aportaba identidad y sumaba ruido). Se mantiene igual en todos los temas.
const RADIUS_CHIP = "18px";
const RADIUS_CARD = "18px";

// Fondos reales de cada tema (styles/globals.css: --color-paper).
const PAPER_LIGHT = "#ffffff";
const PAPER_DARK = "#111111";

export function LodgingTheme({ brand, className, as: Element = "div", children, style: outerStyle, ...props }) {
  const rawId = useId();
  // Clase única y válida para CSS (useId trae ":r1:" y similares).
  const cls = useMemo(() => `hf-brand-${rawId.replace(/[^a-zA-Z0-9_-]/g, "")}`, [rawId]);

  const css = useMemo(() => {
    if (!brand) return "";
    const primary = normalizeHex(brand.primary, "#0fa3a3");
    const accent = normalizeHex(brand.accent, "#d4f53c");
    const tertiary = normalizeHex(brand.tertiary, "#f4b8c4");
    const pattern = PATTERN_IDS.includes(brand.pattern) ? brand.pattern : DEFAULT_PATTERN;
    const [patternMask, patternSize] = patternVars(pattern, 1.4);

    // Variantes para fondo OSCURO: si la marca elegida es muy oscura (azul
    // marino, bordo, negro), la banda y el patrón se aclaran hasta distinguirse
    // del fondo #111. En claro se usan tal cual.
    const primaryDark = visibleOn(primary, PAPER_DARK, 2.4);
    const tertiaryDark = visibleOn(tertiary, PAPER_DARK, 1.55);

    // Variables por tema: en oscuro, las variantes `*-strong` se ACLARAN hasta
    // ser legibles sobre #111 (en claro se oscurecen sobre #fff).
    const vars = (dark) => {
      const bg = dark ? PAPER_DARK : PAPER_LIGHT;
      const hero = dark ? primaryDark : primary;
      const surfaceBase = dark ? tertiaryDark : tertiary;
      return [
        // Marca → cabecera/portada del enlace, datos, íconos y bordes.
        `--color-teal: ${primary};`,
        `--color-teal-strong: ${strongOn(primary, bg, { dark })};`,
        `--color-teal-soft: color-mix(in srgb, ${primary} ${dark ? 22 : 14}%, transparent);`,
        // Acción → botones, precio y selección de servicios.
        `--color-lime: ${accent};`,
        `--color-lime-strong: ${strongOn(accent, bg, { dark })};`,
        `--color-lime-soft: color-mix(in srgb, ${accent} ${dark ? 26 : 22}%, transparent);`,
        `--color-lime-ink: ${readableOn(accent)};`,
        // Fondo → superficie del enlace y acentos suaves.
        `--color-pink: ${tertiary};`,
        `--color-pink-strong: ${strongOn(tertiary, bg, { dark })};`,
        `--color-pink-soft: color-mix(in srgb, ${tertiary} ${dark ? 30 : 25}%, transparent);`,
        // Banda de marca de la cabecera (color plano, sin degradé).
        `--hf-hero: ${hero};`,
        `--hf-hero-ink: ${readableOn(hero)};`,
        // Superficie de la página: el color de fondo mezclado con el papel.
        `--hf-surface: color-mix(in srgb, ${surfaceBase} ${dark ? 14 : 9}%, ${bg});`,
        // Patrón de fondo: máscara con formas (hojas, olas, montañas…) pintada
        // con el color de marca a baja opacidad: se intuye, no compite.
        `--hf-pattern-color: color-mix(in srgb, ${hero} ${dark ? 18 : 13}%, transparent);`,
        `--hf-pattern-mask: ${patternMask};`,
        `--hf-pattern-size: ${patternSize};`,
        // Forma de los elementos (fija).
        `--radius-chip: ${RADIUS_CHIP};`,
        `--radius-card: ${RADIUS_CARD};`,
      ].join("");
    };

    return `.${cls}{${vars(false)}}
.${cls}.dark,.dark .${cls}{${vars(true)}}`;
  }, [cls, brand?.primary, brand?.accent, brand?.tertiary, brand?.pattern]);

  // Sin marca no hay variables que aplicar, pero el contenedor (y su estilo
  // externo, p. ej. el gradiente de la vista previa) se conserva.
  return (
    <Element className={cn(className)} style={outerStyle} {...props}>
      {brand && <style data-hf-brand>{css}</style>}
      <div className={brand ? cls : undefined} style={{ display: "contents" }}>
        {children}
      </div>
    </Element>
  );
}

export default LodgingTheme;

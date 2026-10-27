// Aplica la identidad visual (hasta 3 colores personalizados + forma) de UN
// alojamiento sobre el sistema de temas existente.
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
import { normalizeHex, readableOn, strongOn } from "@utils/color";
import { cn } from "@utils/format";

// Forma de los elementos → radio efectivo (coincide con los presets del panel).
const RADII = { rounded: "18px", soft: "12px", sharp: "8px", pill: "9999px" };

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
    const radius = RADII[brand.radius] ? brand.radius : "rounded";

    // Variables por tema: en oscuro, las variantes `*-strong` se ACLARAN hasta
    // ser legibles sobre #111 (en claro se oscurecen sobre #fff).
    const vars = (dark) => {
      const bg = dark ? PAPER_DARK : PAPER_LIGHT;
      return [
        // Acción secundaria / datos → color principal del alojamiento.
        `--color-teal: ${primary};`,
        `--color-teal-strong: ${strongOn(primary, bg, { dark })};`,
        `--color-teal-soft: color-mix(in srgb, ${primary} ${dark ? 22 : 14}%, transparent);`,
        // Acción primaria (botones) → color de acento.
        `--color-lime: ${accent};`,
        `--color-lime-strong: ${strongOn(accent, bg, { dark })};`,
        `--color-lime-soft: color-mix(in srgb, ${accent} ${dark ? 26 : 22}%, transparent);`,
        `--color-lime-ink: ${readableOn(accent)};`,
        // Acentos suaves → tercer color.
        `--color-pink: ${tertiary};`,
        `--color-pink-strong: ${strongOn(tertiary, bg, { dark })};`,
        `--color-pink-soft: color-mix(in srgb, ${tertiary} ${dark ? 30 : 25}%, transparent);`,
        // Forma de los elementos.
        `--radius-chip: ${RADII[radius]};`,
        `--radius-card: ${radius === "pill" ? "26px" : RADII[radius]};`,
      ].join("");
    };

    return `.${cls}{${vars(false)}}
.${cls}.dark,.dark .${cls}{${vars(true)}}`;
  }, [cls, brand?.primary, brand?.accent, brand?.tertiary, brand?.radius]);

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

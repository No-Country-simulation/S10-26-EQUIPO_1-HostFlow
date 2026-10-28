import { useEffect, useMemo, useRef, useState } from "react";
import { Building2, Check, ImagePlus, Moon, Palette, Pipette, RotateCcw, Sparkles, Sun, Trash2, Undo2, Wand2 } from "lucide-react";
import { Alert, Badge, BrandLogo, Button, Card, Field, Switch } from "@components/ui";
import { LodgingTheme } from "@lib/theme/LodgingTheme";
import { PATTERNS, patternPreviewStyle } from "@lib/theme/patterns";
import { extractPaletteFromImage, suggestBrandTrio } from "@lib/theme/extractPalette";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { BRAND_DEFAULTS } from "@lib/state/store";
import { colorDistance, contrastRatio, hexToRgb, lighten, normalizeHex, readableOn, strongOn } from "@utils/color";
import { cn } from "@utils/format";

// Muestras sugeridas: colores con vida, sin negro ni grises (no aportan
// identidad y ensucian la marca del alojamiento).
const COLOR_PRESETS = [
  { name: "Teal", value: "#0fa3a3" },
  { name: "Lima", value: "#d4f53c" },
  { name: "Rosa", value: "#f4b8c4" },
  { name: "Azul", value: "#3b82f6" },
  { name: "Violeta", value: "#8b5cf6" },
  { name: "Terracota", value: "#e07a5f" },
];

// Los 3 colores tienen un ROL fijo: el dueño elige el color, no qué pinta.
const SLOTS = [
  { id: "primary", label: "Marca", where: "Cabecera y portada del enlace, logo y datos." },
  { id: "accent", label: "Acción", where: "Botones, precio y servicios seleccionados." },
  { id: "tertiary", label: "Fondo", where: "Superficie del enlace y acentos suaves." },
];

// Umbral de jerarquía: por debajo de esta distancia (0-255) dos colores se
// perciben casi iguales y la marca pierde jerarquía visual.
const MIN_DISTANCE = 60;
const UNDO_MS = 6000;

// Editor de UN color: 6 muestras sin negro + color libre (círculo con
// cuentagotas) y medidor de contraste WCAG en vivo. Siempre 1 de los 3 roles.
function SlotEditor({ slot, brand, onPick }) {
  const hex = normalizeHex(brand[slot.id], BRAND_DEFAULTS[slot.id]);
  const ratioWhite = contrastRatio(hex, "#ffffff");
  const ratioInk = contrastRatio(hex, "#111111");
  const best = Math.max(ratioWhite, ratioInk);
  const aa = best >= 4.5;

  return (
    <div className="rounded-2xl border border-line bg-cloud p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-sm font-bold text-ink">Color de {slot.label.toLowerCase()}</p>
          <p className="mt-0.5 text-[11px] leading-snug text-muted">{slot.where}</p>
        </div>
        <span
          className="size-10 shrink-0 rounded-full border border-line-strong"
          style={{ backgroundColor: hex }}
          aria-hidden="true"
        />
      </div>

      {/* Muestras circulares: el seleccionado se marca con un anillo, no con un borde cuadrado */}
      <div className="mt-3 flex flex-wrap items-center gap-2.5">
        {COLOR_PRESETS.map((preset) => {
          const active = hex === preset.value.toLowerCase();
          return (
            <button
              key={preset.value}
              type="button"
              onClick={() => onPick(preset.value)}
              title={preset.name}
              aria-pressed={active}
              aria-label={`Color ${preset.name} para ${slot.label.toLowerCase()}`}
              className="grid size-9 cursor-pointer place-items-center rounded-full transition-transform hover:scale-110 focus-visible:outline-offset-4"
              style={{
                backgroundColor: preset.value,
                boxShadow: active
                  ? `0 0 0 2px var(--color-paper), 0 0 0 4px ${preset.value}`
                  : "inset 0 0 0 1px rgba(17, 17, 17, 0.12)",
              }}
            >
              {active && <Check size={16} strokeWidth={3} style={{ color: readableOn(preset.value) }} />}
            </button>
          );
        })}

        {/* Color libre: mismo círculo, abre el selector nativo */}
        <label
          className="relative grid size-9 cursor-pointer place-items-center rounded-full border border-line-strong"
          style={{ backgroundColor: hex }}
          title="Elegir un color libre"
        >
          <input
            type="color"
            value={hex}
            onChange={(event) => onPick(event.target.value)}
            aria-label={`Elegir color libre para ${slot.label.toLowerCase()}`}
            className="absolute inset-0 size-full cursor-pointer opacity-0"
          />
          <Pipette size={14} aria-hidden="true" style={{ color: readableOn(hex) }} />
        </label>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        <span className="font-mono text-xs text-ink-soft">{hex}</span>
        <span
          className={cn(
            "rounded-lg px-2 py-1 text-[11px] font-bold",
            aa ? "bg-teal-soft text-teal-strong" : "bg-danger-soft text-danger",
          )}
        >
          {best.toFixed(1)}:1 {aa ? "AA ✓" : "⚠ mínimo 4.5:1"}
        </span>
        <Button
          type="button"
          variant="ghost"
          icon={Wand2}
          onClick={() => onPick(strongOn(hex, "#ffffff"))}
          title="Ajusta el tono hasta cumplir el contraste mínimo (AA) como texto sobre fondo claro"
          className="min-h-[32px] px-2.5 text-[11px]"
        >
          Sugerir tono legible
        </Button>
      </div>
      <p className="mt-1.5 text-[11px] text-muted">
        Texto blanco {ratioWhite.toFixed(1)}:1 · tinta oscura {ratioInk.toFixed(1)}:1 sobre este color.
      </p>
    </div>
  );
}

export default function LodgingProfilePage() {
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const [saved, setSaved] = useState(false);
  const fileRef = useRef(null);

  // Colores extraídos del logo: se PERSISTEN en lodging.brandPalette para que
  // sobrevivan a recargas (mismo ciclo de vida que el logo).
  const [palette, setPalette] = useState(() => (Array.isArray(lodging.brandPalette) ? lodging.brandPalette : []));
  const [extracting, setExtracting] = useState(false);
  const [paletteError, setPaletteError] = useState("");
  const [target, setTarget] = useState("primary");
  // Vista previa en claro/oscuro: SOLO dentro del preview (clase .dark acotada).
  const [previewDark, setPreviewDark] = useState(false);
  const [previewSwitch, setPreviewSwitch] = useState(true);
  // Aplicar paleta completa: preview al pasar el cursor + deshacer por 6 s.
  const [trioPreview, setTrioPreview] = useState(null);
  const [undoBrand, setUndoBrand] = useState(null);
  // Restaurar marca por defecto: confirmación en 2 pasos.
  const [resetArmed, setResetArmed] = useState(false);

  useEffect(() => {
    if (!undoBrand) return undefined;
    const timer = setTimeout(() => setUndoBrand(null), UNDO_MS);
    return () => clearTimeout(timer);
  }, [undoBrand]);

  const flashSaved = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2400);
  };

  // Lee la paleta dominante de una imagen (data URL), la deja como sugerencia
  // y la persiste junto al alojamiento.
  const extractFrom = async (src) => {
    setExtracting(true);
    setPaletteError("");
    try {
      const colors = await extractPaletteFromImage(src);
      setPalette(colors);
      actions.updateLodging({ brandPalette: colors });
    } catch (error) {
      setPalette([]);
      actions.updateLodging({ brandPalette: [] });
      setPaletteError(error?.message || "No se pudieron leer los colores del logo.");
    } finally {
      setExtracting(false);
    }
  };

  const extractFromLogo = () => {
    if (lodging.logo) extractFrom(lodging.logo);
  };

  const trio = useMemo(() => (palette.length ? suggestBrandTrio(palette) : null), [palette]);

  // Aplica el trío sugerido guardando el estado anterior (para Deshacer).
  const applyTrio = () => {
    if (!trio) return;
    setUndoBrand({ ...lodging.brand });
    actions.updateBrand(trio);
    setTrioPreview(null);
    flashSaved();
  };

  const undoTrio = () => {
    if (undoBrand) actions.updateBrand(undoBrand);
    setUndoBrand(null);
    flashSaved();
  };

  const restoreDefaults = () => {
    actions.updateBrand({ ...BRAND_DEFAULTS });
    setResetArmed(false);
    flashSaved();
  };

  // Color legible de un slot (tinta sugerida para textos sobre ese color).
  const pickSlot = (slotId) => (hex) => {
    actions.updateBrand({ [slotId]: normalizeHex(hex) });
    flashSaved();
  };

  // ¿En qué slots de marca ya se usa este color de la paleta?
  const slotsUsing = (hex) =>
    SLOTS.filter((slot) => String(lodging.brand[slot.id] || "").toLowerCase() === hex.toLowerCase())
      .map((slot) => slot.label)
      .join(" · ");

  // Jerarquía: si dos slots quedan casi iguales, la marca pierde jerarquía.
  const clashes = useMemo(() => {
    const out = [];
    for (let i = 0; i < SLOTS.length; i += 1) {
      for (let j = i + 1; j < SLOTS.length; j += 1) {
        const a = SLOTS[i];
        const b = SLOTS[j];
        const ha = hexToRgb(normalizeHex(lodging.brand[a.id], BRAND_DEFAULTS[a.id]));
        const hb = normalizeHex(lodging.brand[b.id], BRAND_DEFAULTS[b.id]);
        // colorDistance trabaja con objetos RGB ({r,g,b}).
        if (colorDistance(ha, hexToRgb(hb)) < MIN_DISTANCE) {
          // Sugerencia: aclarar el segundo color hasta distinguirse.
          out.push({ a, b, suggestion: lighten(hb, 0.4), current: hb });
        }
      }
    }
    return out;
  }, [lodging.brand.primary, lodging.brand.accent, lodging.brand.tertiary]);

  // Marca efectiva del preview (con trío en hover, si hay).
  const previewBrand = trioPreview ? { ...lodging.brand, ...trioPreview } : lodging.brand;
  // Fotos reales del alojamiento (0 a 3 en la vista previa).
  const previewImages = (Array.isArray(lodging.images) ? lodging.images : []).slice(0, 3);

  const submit = (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    actions.updateLodging({
      name: String(form.get("name") || "").trim(),
      tagline: String(form.get("tagline") || "").trim(),
      location: String(form.get("location") || "").trim(),
      phone: String(form.get("phone") || "").trim(),
    });
    flashSaved();
  };

  const pickLogo = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = String(reader.result);
      actions.setLogo(dataUrl);
      flashSaved();
      // Al subir un logo nuevo se sugieren sus colores automáticamente.
      extractFrom(dataUrl);
    };
    reader.readAsDataURL(file);
    event.target.value = "";
  };

  const removeLogo = () => {
    actions.setLogo(null);
    setPalette([]);
    actions.updateLodging({ brandPalette: [] });
    flashSaved();
  };

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Perfil y personalización</h1>
          <p className="mt-1 text-sm text-muted">Subí tu logo y adaptá la identidad de tu empresa.</p>
        </div>
        {saved && <Alert tone="success">Perfil actualizado.</Alert>}
      </header>

      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        {/* Logo */}
        <Card className="p-6">
          <div className="mb-4 flex items-center gap-2">
            <span className="grid size-10 place-items-center rounded-xl bg-teal-soft text-teal-strong">
              <ImagePlus size={20} />
            </span>
            <div>
              <h2 className="font-bold text-ink">Logo de la empresa</h2>
              <p className="text-xs text-muted">Se muestra en el panel y en el enlace de reserva.</p>
            </div>
          </div>

          <div className="grid gap-3 rounded-2xl border border-dashed border-line-strong bg-cloud p-4">
            <div className="grid place-items-center gap-3 px-2 py-5 text-center">
              <BrandLogo src={lodging.logo} name={lodging.name} size="lg" />
              <div>
                <p className="text-sm font-bold text-ink">{lodging.name}</p>
                <p className="text-xs text-muted">
                  {lodging.logo
                    ? "Tu logo completo, sin recortes ni deformaciones."
                    : "Sin logo: se muestran tus iniciales con el color de marca."}
                </p>
              </div>
            </div>

            {/* Así se ve en la cabecera del enlace del huésped */}
            <LodgingTheme brand={lodging.brand}>
              <div
                className="flex items-center gap-2.5 rounded-xl px-3 py-2.5"
                style={{ backgroundColor: "var(--hf-hero)", color: "var(--hf-hero-ink)" }}
              >
                <BrandLogo src={lodging.logo} name={lodging.name} size="sm" />
                <div className="min-w-0">
                  <p className="truncate text-sm font-extrabold">{lodging.name}</p>
                  <p className="truncate text-[10px] font-semibold opacity-80">Enlace de llegada</p>
                </div>
              </div>
            </LodgingTheme>
            <p className="text-center text-[11px] font-semibold text-muted">Así se ve en la cabecera del enlace</p>
          </div>

          <div className="mt-4 flex gap-2">
            <Button type="button" variant="secondary" icon={ImagePlus} className="flex-1" onClick={() => fileRef.current?.click()}>
              {lodging.logo ? "Cambiar logo" : "Subir logo"}
            </Button>
            {lodging.logo && (
              <Button type="button" variant="ghost" icon={Trash2} onClick={removeLogo}>
                Quitar
              </Button>
            )}
          </div>
          <input ref={fileRef} type="file" accept="image/*" hidden onChange={pickLogo} />
          <p className="mt-3 text-xs text-muted">
            PNG, JPG o SVG con fondo transparente. Se respeta la proporción del archivo: cuadrado, horizontal o
            vertical.
          </p>

          {/* Colores sugeridos desde el logo (extraídos en el navegador, sin enviar nada) */}
          {lodging.logo && (
            <div className="mt-4 rounded-2xl border border-line bg-cloud p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="flex items-center gap-1.5 text-sm font-bold text-ink">
                  <Sparkles size={15} className="text-teal-strong" />
                  Colores de tu logo
                </p>
                <Button
                  type="button"
                  variant="secondary"
                  icon={Sparkles}
                  onClick={extractFromLogo}
                  disabled={extracting}
                  aria-busy={extracting}
                  className="min-h-[38px] px-3.5 text-xs"
                >
                  {extracting ? "Analizando…" : palette.length ? "Re-analizar" : "Extraer colores"}
                </Button>
              </div>

              {!palette.length && !paletteError && (
                <p className="mt-2 text-xs text-muted">
                  Extraé los colores dominantes de tu logo para usarlos como marca. El análisis se hace en tu
                  navegador: la imagen nunca se envía a ningún servidor.
                </p>
              )}

              {paletteError && <Alert>{paletteError}</Alert>}

              {palette.length > 0 && (
                <div className="mt-3 grid gap-3">
                  {/* Trío sugerido: se ve antes de aplicarlo y con preview en hover */}
                  {trio && (
                    <div className="flex flex-wrap items-center gap-2 rounded-xl border border-line bg-paper p-2">
                      <span className="text-xs font-bold text-ink">Sugerido:</span>
                      {SLOTS.map((slot) => (
                        <span
                          key={slot.id}
                          className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1 text-[11px] font-bold text-ink-soft"
                          style={{ backgroundColor: trio[slot.id] }}
                        >
                          <span style={{ color: readableOn(trio[slot.id]) }}>{slot.label}</span>
                          <span style={{ color: readableOn(trio[slot.id]) }} className="font-mono opacity-80">
                            {trio[slot.id]}
                          </span>
                        </span>
                      ))}
                      <Button
                        type="button"
                        variant="ghost"
                        onMouseEnter={() => setTrioPreview(trio)}
                        onFocus={() => setTrioPreview(trio)}
                        onMouseLeave={() => setTrioPreview(null)}
                        onBlur={() => setTrioPreview(null)}
                        onClick={applyTrio}
                        className="min-h-[34px] px-3 text-xs"
                      >
                        Aplicar paleta completa
                      </Button>
                    </div>
                  )}

                  {/* Deshacer (6 s) tras aplicar la paleta */}
                  {undoBrand && (
                    <Alert tone="info">
                      <span className="flex flex-wrap items-center gap-2">
                        Paleta aplicada a los 3 colores.
                        <Button type="button" variant="ghost" icon={Undo2} onClick={undoTrio} className="min-h-[30px] px-2.5 text-xs">
                          Deshacer
                        </Button>
                      </span>
                    </Alert>
                  )}

                  {/* Swatches detectados: tocando uno se aplica al slot elegido */}
                  <div className="flex flex-wrap gap-2">
                    {palette.map((hex) => {
                      const usedIn = slotsUsing(hex);
                      return (
                        <button
                          key={hex}
                          type="button"
                          onClick={() => { actions.updateBrand({ [target]: hex }); flashSaved(); }}
                          title={usedIn ? `${hex} · en uso (${usedIn})` : `${hex} · aplicar como ${SLOTS.find((s) => s.id === target).label.toLowerCase()}`}
                          aria-label={usedIn ? `Color ${hex} en uso (${usedIn})` : `Aplicar color ${hex} como ${SLOTS.find((s) => s.id === target).label.toLowerCase()}`}
                          aria-pressed={Boolean(usedIn)}
                          className="grid size-11 cursor-pointer place-items-center rounded-xl border-2 transition-all hover:scale-105"
                          style={{ backgroundColor: hex, borderColor: usedIn ? "var(--color-ink)" : "transparent" }}
                        >
                          {usedIn && <Check size={18} aria-hidden="true" style={{ color: readableOn(hex) }} />}
                        </button>
                      );
                    })}
                  </div>

                  {/* Slot de destino */}
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs text-muted">Aplicar como:</span>
                    <div className="inline-flex rounded-xl border border-line bg-paper p-1">
                      {SLOTS.map((slot) => (
                        <button
                          key={slot.id}
                          type="button"
                          onClick={() => setTarget(slot.id)}
                          aria-pressed={target === slot.id}
                          className={cn(
                            "cursor-pointer rounded-lg px-3 py-1.5 text-xs font-bold transition",
                            target === slot.id ? "bg-teal text-white" : "text-ink-soft hover:text-ink",
                          )}
                        >
                          {slot.label}
                        </button>
                      ))}
                    </div>
                  </div>
                  <p className="text-[11px] text-muted">
                    Tocá un color para asignarlo al slot elegido. Tu paleta queda guardada junto al logo.
                  </p>
                </div>
              )}
            </div>
          )}
        </Card>

        {/* Datos de la empresa */}
        <Card className="p-6">
          <div className="mb-4 flex items-center gap-2">
            <span className="grid size-10 place-items-center rounded-xl bg-lime-soft text-lime-ink">
              <Building2 size={20} />
            </span>
            <div>
              <h2 className="font-bold text-ink">Datos de la empresa</h2>
              <p className="text-xs text-muted">Información que ven tus huéspedes.</p>
            </div>
          </div>

          <form onSubmit={submit} className="grid gap-4">
            <Field label="Nombre de la empresa" name="name" defaultValue={lodging.name} />
            <Field label="Eslogan" name="tagline" defaultValue={lodging.tagline} placeholder="Ej. Descanso frente al río" />
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Ubicación" name="location" defaultValue={lodging.location} />
              <Field label="Teléfono" name="phone" defaultValue={lodging.phone} />
            </div>
            <Button type="submit" className="mt-1 w-full sm:w-auto">Guardar datos</Button>
          </form>
        </Card>
      </div>

      {/* Marca */}
      <Card className="p-6">
        <div className="mb-5 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span className="grid size-10 place-items-center rounded-xl bg-pink-soft text-pink-strong">
              <Palette size={20} />
            </span>
            <div>
              <h2 className="font-bold text-ink">Personalización visual</h2>
              <p className="text-xs text-muted">3 colores con rol, forma de las esquinas y patrón del enlace de llegada.</p>
            </div>
          </div>
          {/* Restaurar marca por defecto: confirmación en 2 pasos */}
          {resetArmed ? (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-bold text-danger">¿Restaurar los colores originales?</span>
              <Button type="button" variant="ghost" onClick={restoreDefaults} className="min-h-[34px] px-3 text-xs">
                Sí, restaurar
              </Button>
              <Button type="button" variant="ghost" onClick={() => setResetArmed(false)} className="min-h-[34px] px-3 text-xs">
                Cancelar
              </Button>
            </div>
          ) : (
            <Button
              type="button"
              variant="ghost"
              icon={RotateCcw}
              onClick={() => { setResetArmed(true); setTimeout(() => setResetArmed(false), 5000); }}
              className="min-h-[36px] px-3 text-xs"
            >
              Restaurar marca
            </Button>
          )}
        </div>

        {/* Jerarquía de los 3 colores */}
        {clashes.map(({ a, b, suggestion }) => (
          <Alert key={`${a.id}-${b.id}`} tone="info">
            <span className="flex flex-wrap items-center gap-2">
              Los colores {a.label.toLowerCase()} y {b.label.toLowerCase()} se parecen demasiado: la marca pierde
              jerarquía.
              <Button
                type="button"
                variant="ghost"
                onClick={() => { actions.updateBrand({ [b.id]: suggestion }); flashSaved(); }}
                className="min-h-[30px] px-2.5 text-xs"
              >
                Usar {suggestion} en {b.label.toLowerCase()}
              </Button>
            </span>
          </Alert>
        ))}

        <div className="mt-4 grid gap-6 lg:grid-cols-2">
          <div className="grid gap-5">
            {SLOTS.map((slot) => (
              <SlotEditor key={slot.id} slot={slot} brand={lodging.brand} onPick={pickSlot(slot.id)} />
            ))}

            {/* Patrón de fondo del enlace: motivos elegantes, sin imágenes */}
            <div>
              <p className="mb-1 text-sm font-bold text-ink">Patrón de fondo</p>
              <p className="mb-2 text-[11px] text-muted">
                Motivos dibujados con tu color de marca sobre el fondo del enlace. Se adaptan solos al modo claro y
                oscuro.
              </p>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {PATTERNS.map((pattern) => {
                  const active = (lodging.brand.pattern || "none") === pattern.id;
                  return (
                    <button
                      key={pattern.id}
                      type="button"
                      onClick={() => { actions.updateBrand({ pattern: pattern.id }); flashSaved(); }}
                      aria-pressed={active}
                      aria-label={`Patrón ${pattern.label}`}
                      className={cn(
                        "relative h-16 cursor-pointer overflow-hidden rounded-xl border bg-paper text-ink transition",
                        active ? "border-teal ring-2 ring-teal/25" : "border-line hover:border-line-strong",
                      )}
                    >
                      <span
                        aria-hidden="true"
                        className="pointer-events-none absolute inset-0 text-ink opacity-25"
                        style={patternPreviewStyle(pattern.id)}
                      />
                      <span className="absolute inset-x-0 bottom-0 bg-paper/90 py-1 text-[10px] font-bold">
                        {pattern.label}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Vista previa: componentes REALES con la marca (y trío en hover) */}
          <div className="grid content-start gap-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-bold text-ink">
                Vista previa para tus huéspedes
                {trioPreview && <span className="ml-2 text-xs font-medium text-teal-strong">(sugerido)</span>}
              </p>
              <button
                type="button"
                onClick={() => setPreviewDark((current) => !current)}
                aria-pressed={previewDark}
                aria-label={`Vista previa en modo ${previewDark ? "oscuro" : "claro"}`}
                className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong px-3 py-1.5 text-xs font-bold text-ink-soft transition hover:border-ink/40 hover:text-ink"
              >
                {previewDark ? <Moon size={13} aria-hidden="true" /> : <Sun size={13} aria-hidden="true" />}
                {previewDark ? "Oscuro" : "Claro"}
              </button>
            </div>

            <LodgingTheme
              brand={previewBrand}
              className={cn("overflow-hidden rounded-2xl border border-line bg-paper shadow-card", previewDark && "dark")}
            >
              <div className="hf-surface">
                {/* Banda de marca: lo primero que ve el huésped */}
                <div
                  className="flex items-center gap-2.5 px-4 py-3"
                  style={{ backgroundColor: "var(--hf-hero)", color: "var(--hf-hero-ink)" }}
                >
                  <BrandLogo src={lodging.logo} name={lodging.name} size="sm" />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-extrabold">{lodging.name}</p>
                    <p className="truncate text-[10px] font-semibold opacity-80">{lodging.tagline}</p>
                  </div>
                  <span
                    className="ml-auto shrink-0 rounded-full px-2 py-0.5 text-[9px] font-bold"
                    style={{
                      backgroundColor: "color-mix(in srgb, var(--hf-hero-ink) 16%, transparent)",
                      color: "var(--hf-hero-ink)",
                    }}
                  >
                    Huésped
                  </span>
                </div>
                <div className="h-[3px] w-full" style={{ backgroundColor: "var(--color-lime)" }} />

                {/* Fotos del alojamiento (las mismas de /panel/alojamiento) */}
                {previewImages.length > 0 && (
                  <div className="flex gap-1.5 px-4 pt-4">
                    {previewImages.map((src, index) => (
                      <img
                        key={index}
                        src={src}
                        alt=""
                        aria-hidden="true"
                        className="h-14 w-20 shrink-0 rounded-[var(--radius-chip)] border border-line object-cover"
                      />
                    ))}
                  </div>
                )}

                {/* Contenido real con la marca aplicada */}
                <div className="grid gap-3 p-4">
                  <div className="card grid gap-2.5 p-3.5">
                    <div>
                      <p className="text-sm font-extrabold text-ink">¡Hola, María!</p>
                      <p className="mt-0.5 text-[11px] text-muted">Completá tu ficha de llegada para {lodging.name}.</p>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      <Badge tone="teal">Marca</Badge>
                      <Badge tone="lime">Acción</Badge>
                      <Badge tone="pink">Fondo</Badge>
                    </div>
                    <Field label="DNI / Pasaporte" placeholder="30.123.456" readOnly />
                    <Switch
                      checked={previewSwitch}
                      onChange={setPreviewSwitch}
                      label="Desayuno por noche"
                      description="Se suma al total de la estadía"
                    />
                    <Alert tone="lime">Tu solicitud de lancha quedó registrada.</Alert>
                    <div className="flex flex-wrap gap-2">
                      <Button type="button">Continuar</Button>
                      <Button type="button" variant="secondary">Ver detalles</Button>
                    </div>
                  </div>
                </div>
              </div>
            </LodgingTheme>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-3">
          <Badge tone="neutral">Tema: automático (claro/oscuro)</Badge>
          <span className="text-xs text-muted">
            Los 3 colores tienen un rol fijo (marca, acción y fondo) y se adaptan solos al modo oscuro. Se aplican al
            enlace de tus huéspedes y a esta vista previa: el panel queda neutro.
          </span>
        </div>
      </Card>
    </div>
  );
}

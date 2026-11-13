import { useEffect, useMemo, useRef, useState } from "react";
import { Alert } from "@components/ui";
import { LogoCard } from "../components/LogoCard";
import { CompanyDataCard } from "../components/CompanyDataCard";
import { BrandSettingsCard } from "../components/BrandSettingsCard";
import { MIN_DISTANCE, SLOTS, UNDO_MS } from "../lib/brandEditor";
import { extractPaletteFromImage, suggestBrandTrio } from "@lib/theme/extractPalette";
import { useLodging } from "@lib/api/hooks";
import { useSession } from "@features/auth";
import { BRAND_DEFAULTS } from "@lib/domain/lodging";
import { colorDistance, hexToRgb, lighten, normalizeHex } from "@utils/color";

export default function LodgingProfilePage() {
  const { actions } = useSession();
  const lodging = useLodging();
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
        <LogoCard
          lodging={lodging}
          fileRef={fileRef}
          pickLogo={pickLogo}
          removeLogo={removeLogo}
          extracting={extracting}
          palette={palette}
          extractFromLogo={extractFromLogo}
          paletteError={paletteError}
          trio={trio}
          applyTrio={applyTrio}
          setTrioPreview={setTrioPreview}
          undoBrand={undoBrand}
          undoTrio={undoTrio}
          target={target}
          setTarget={setTarget}
          actions={actions}
          flashSaved={flashSaved}
        />

        {/* Datos de la empresa */}
        <CompanyDataCard lodging={lodging} submit={submit} />
      </div>

      {/* Marca */}
      <BrandSettingsCard
        lodging={lodging}
        resetArmed={resetArmed}
        setResetArmed={setResetArmed}
        restoreDefaults={restoreDefaults}
        clashes={clashes}
        pickSlot={pickSlot}
        actions={actions}
        flashSaved={flashSaved}
        trioPreview={trioPreview}
        previewDark={previewDark}
        setPreviewDark={setPreviewDark}
        previewSwitch={previewSwitch}
        setPreviewSwitch={setPreviewSwitch}
      />
    </div>
  );
}

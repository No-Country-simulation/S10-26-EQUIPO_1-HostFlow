import { Check, ImagePlus, Sparkles, Trash2, Undo2 } from "lucide-react";
import { Alert, BrandLogo, Button, Card } from "@components/ui";
import { LodgingTheme } from "@lib/theme/LodgingTheme";
import { SLOTS } from "../lib/brandEditor";
import { readableOn } from "@utils/color";
import { cn } from "@utils/format";

export function LogoCard({
  lodging,
  fileRef,
  pickLogo,
  removeLogo,
  extracting,
  palette,
  extractFromLogo,
  paletteError,
  trio,
  applyTrio,
  setTrioPreview,
  undoBrand,
  undoTrio,
  target,
  setTarget,
  actions,
  flashSaved,
}) {
  // ¿En qué slots de marca ya se usa este color de la paleta?
  const slotsUsing = (hex) =>
    SLOTS.filter((slot) => String(lodging.brand[slot.id] || "").toLowerCase() === hex.toLowerCase())
      .map((slot) => slot.label)
      .join(" · ");

  return (
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
                ? "Se muestra en círculo: el motivo principal queda centrado."
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
        PNG, JPG o SVG. El logo se muestra en un círculo, así que conviene que el motivo esté centrado.
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
  );
}

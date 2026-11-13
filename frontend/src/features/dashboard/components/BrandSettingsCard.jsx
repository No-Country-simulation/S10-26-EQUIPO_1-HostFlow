import { Moon, Palette, RotateCcw, Sun } from "lucide-react";
import { Alert, Badge, BrandLogo, Button, Card, Field, Switch } from "@components/ui";
import { LodgingTheme } from "@lib/theme/LodgingTheme";
import { PATTERNS, patternPreviewStyle } from "@lib/theme/patterns";
import { SLOTS } from "../lib/brandEditor";
import { SlotEditor } from "./SlotEditor";
import { cn } from "@utils/format";

export function BrandSettingsCard({
  lodging,
  resetArmed,
  setResetArmed,
  restoreDefaults,
  clashes,
  pickSlot,
  actions,
  flashSaved,
  trioPreview,
  previewDark,
  setPreviewDark,
  previewSwitch,
  setPreviewSwitch,
}) {
  // Marca efectiva del preview (con trío en hover, si hay).
  const previewBrand = trioPreview ? { ...lodging.brand, ...trioPreview } : lodging.brand;
  // Fotos reales del alojamiento (0 a 3 en la vista previa).
  const previewImages = (Array.isArray(lodging.images) ? lodging.images : []).slice(0, 3);

  return (
    <Card className="p-6">
      <div className="mb-5 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="grid size-10 place-items-center rounded-xl bg-pink-soft text-pink-strong">
            <Palette size={20} />
          </span>
          <div>
            <h2 className="font-bold text-ink">Personalización visual</h2>
            <p className="text-xs text-muted">3 colores con rol y patrón del enlace de llegada.</p>
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

          {/* Patrón de fondo del enlace: 3 motivos, sin imágenes. Tocar el
              activo lo quita (queda liso). */}
          <div>
            <p className="mb-1 text-sm font-bold text-ink">Patrón de fondo</p>
            <p className="mb-2 text-[11px] text-muted">
              Tres motivos dibujados con tu color de marca. Se adaptan solos al modo claro y oscuro; tocá el que
              está activo para dejarlo liso.
            </p>
            <div className="grid grid-cols-3 gap-2">
              {PATTERNS.filter((pattern) => pattern.id !== "none").map((pattern) => {
                const active = (lodging.brand.pattern || "none") === pattern.id;
                return (
                  <button
                    key={pattern.id}
                    type="button"
                    onClick={() => {
                      actions.updateBrand({ pattern: active ? "none" : pattern.id });
                      flashSaved();
                    }}
                    aria-pressed={active}
                    aria-label={`Patrón ${pattern.label}${active ? " (tocar para quitar)" : ""}`}
                    className={cn(
                      "relative h-16 cursor-pointer overflow-hidden rounded-xl border bg-paper text-ink transition",
                      active ? "border-teal ring-2 ring-teal/25" : "border-line hover:border-line-strong",
                    )}
                  >
                    <span
                      aria-hidden="true"
                      className={cn(
                        "pointer-events-none absolute inset-0 text-ink",
                        active ? "opacity-35" : "opacity-20",
                      )}
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
              Vista previa del enlace y del panel
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

                {/* El panel del anfitrión usa la misma marca: navegación activa,
                    botón primario y superficie tintada con el patrón. */}
                <div className="rounded-[var(--radius-card)] border border-line bg-paper p-3">
                  <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-muted">
                    Panel del anfitrión
                  </p>
                  <div className="mt-2 flex flex-wrap items-center gap-2">
                    <span className="rounded-lg bg-lime px-2.5 py-1 text-[10px] font-extrabold text-lime-ink">
                      Reservas
                    </span>
                    <span className="rounded-lg border border-line px-2.5 py-1 text-[10px] font-bold text-ink-soft">
                      Alojamiento
                    </span>
                    <span className="rounded-lg border border-line px-2.5 py-1 text-[10px] font-bold text-ink-soft">
                      Pagos
                    </span>
                  </div>
                  <div className="mt-2.5 flex items-center justify-between gap-2 rounded-xl bg-teal-soft px-3 py-2">
                    <span className="text-[11px] font-bold text-teal-strong">3 llegadas esta semana</span>
                    <Button type="button" className="min-h-[30px] px-2.5 text-[10px]">
                      Ver reservas
                    </Button>
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
          enlace de tus huéspedes y a tu panel: navegación, botones y superficies usan la misma identidad.
        </span>
      </div>
    </Card>
  );
}

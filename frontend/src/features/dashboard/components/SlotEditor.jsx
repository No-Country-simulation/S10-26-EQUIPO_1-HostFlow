import { Check, Pipette, Wand2 } from "lucide-react";
import { Button } from "@components/ui";
import { BRAND_DEFAULTS } from "@lib/domain/lodging";
import { contrastRatio, normalizeHex, readableOn, strongOn } from "@utils/color";
import { cn } from "@utils/format";
import { COLOR_PRESETS } from "../lib/brandEditor";

// Editor de UN color: 6 muestras sin negro + color libre (círculo con
// cuentagotas) y medidor de contraste WCAG en vivo. Siempre 1 de los 3 roles.
export function SlotEditor({ slot, brand, onPick }) {
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

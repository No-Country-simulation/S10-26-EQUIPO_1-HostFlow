import { Anchor, Ship, X } from "lucide-react";
import { Button, Field } from "@components/ui";
import { cn, toISODate } from "@utils/format";

// Paso opcional de cotización de transporte en lancha.
// El huésped elige si quiere la cotización:
//   · "No, gracias"  → el onboarding continúa sin solicitarla (declinar).
//   · "Sí, quiero"   → se piden los datos del viaje; después se genera el JSON.
// Los datos se quedan en el paso hasta CONFIRMAR LLEGADA: así una solicitud no
// se genera dos veces por clicks repetidos.
export default function BoatQuoteStep({ quote, onChange, booking, lodging }) {
  const set = (patch) => onChange({ ...quote, ...patch });

  const choose = (wanted) => () => set({ decided: true, wanted });

  return (
    <div className="grid gap-5">
      <div>
        <h2 className="text-xl font-extrabold text-ink">¿Querés cotizar el traslado en lancha?</h2>
        <p className="mt-1.5 text-sm text-muted">
          Si llegás por el río, podés pedir una cotización del servicio de lancha remis hasta{" "}
          <span className="font-semibold text-ink">{lodging.location}</span>. Es opcional: si no la necesitás,
          continuás con tu llegada igual.
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <button
          type="button"
          onClick={choose(true)}
          aria-pressed={quote.decided && quote.wanted}
          className={cn(
            "card card-hover grid cursor-pointer gap-2 p-5 text-left transition-all",
            quote.decided && quote.wanted ? "border-2 border-[color:var(--hf-hero)]" : "hover:border-teal",
          )}
        >
          <span className="grid size-10 w-fit place-items-center rounded-xl bg-teal-soft text-teal-strong">
            <Ship size={19} />
          </span>
          <span className="font-bold text-ink">Sí, quiero la cotización</span>
          <span className="text-xs text-muted">Completás los datos del viaje y te preparamos la solicitud.</span>
        </button>

        <button
          type="button"
          onClick={choose(false)}
          aria-pressed={quote.decided && !quote.wanted}
          className={cn(
            "card card-hover grid cursor-pointer gap-2 p-5 text-left transition-all",
            quote.decided && !quote.wanted ? "border-2 border-line-strong" : "hover:border-line-strong",
          )}
        >
          <span className="grid size-10 w-fit place-items-center rounded-xl bg-overlay text-ink-soft">
            <X size={19} />
          </span>
          <span className="font-bold text-ink">No, gracias</span>
          <span className="text-xs text-muted">Continuás con el resto del onboarding.</span>
        </button>
      </div>

      {quote.decided && quote.wanted && (
        <div className="card grid gap-4 p-5 animate-rise">
          <p className="flex items-center gap-2 font-bold text-ink">
            <Anchor size={17} className="text-teal-strong" /> Datos del traslado
          </p>

          <Field
            label="Lugar de embarque"
            name="pickupPlace"
            value={quote.pickupPlace}
            onChange={(e) => set({ pickupPlace: e.target.value })}
            placeholder="Ej. Terminal fluvial de Tigre"
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              label="Fecha"
              name="pickupDate"
              type="date"
              min={toISODate()}
              value={quote.pickupDate}
              onChange={(e) => set({ pickupDate: e.target.value })}
            />
            <Field
              label="Hora"
              name="pickupTime"
              type="time"
              value={quote.pickupTime}
              onChange={(e) => set({ pickupTime: e.target.value })}
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              label="Pasajeros"
              name="passengers"
              type="number"
              min="1"
              value={quote.passengers}
              onChange={(e) => set({ passengers: e.target.value })}
            />
            <Field
              label="Equipaje"
              name="baggage"
              type="number"
              min="0"
              value={quote.baggage}
              onChange={(e) => set({ baggage: e.target.value })}
              hint="Cantidad de valijas grandes."
            />
          </div>
          <Field
            label="Notas (opcional)"
            as="textarea"
            name="notes"
            value={quote.notes}
            onChange={(e) => set({ notes: e.target.value })}
            placeholder="Ej. Llegamos con una silla de ruedas plegable."
          />

          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-xs text-muted">
              La cotización es una solicitud: no implica una reserva ni un cobro.
            </p>
            <Button type="button" variant="secondary" onClick={() => set({ decided: false, wanted: false })}>
              Cambiar respuesta
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

// Paso 2 del onboarding: alergias (multiselección) y preferencias.
import { Alert, Field } from "@components/ui";
import { Check, HeartPulse, Sparkles } from "lucide-react";
import { cn } from "@utils/format";
import { ALLERGY_OPTIONS, OTHER_ALLERGY } from "../../lib/onboardingForm";
import StepHeading from "../StepHeading";

export default function HealthStep({
  allergies,
  setAllergies,
  allergyOther,
  setAllergyOther,
  health,
  setHealthField,
  error,
  lodging,
}) {
  return (
    <div className="card grid gap-4 overflow-hidden p-5 animate-rise sm:p-6">
      <StepHeading
        icon={HeartPulse}
        eyebrow="Salud y preferencias"
        title="Alérgenos y preferencias"
        text={`Esta información ayuda a ${lodging.name} a recibirte mejor.`}
      />

      <Alert tone="info">Solo la ve {lodging.name} y su equipo: no se comparte con otros huéspedes.</Alert>

      {/* Multiselección: cada alergia marcada se envía dentro del array
          `allergies`. "Otro" abre un campo para lo que no está en la lista. */}
      <fieldset className="grid gap-2.5">
        <legend className="flex items-center gap-1.5 text-sm font-bold text-ink">
          <HeartPulse size={14} className="text-teal-strong" aria-hidden="true" />
          Alergias alimentarias o de otro tipo
        </legend>
        <p className="text-xs text-muted">
          Marcá todas las que tengas. Si no tenés ninguna, dejá la lista sin seleccionar.
        </p>
        <div className="flex flex-wrap gap-2">
          {[...ALLERGY_OPTIONS, OTHER_ALLERGY].map((option) => {
            const active = allergies.includes(option);
            return (
              <button
                key={option}
                type="button"
                aria-pressed={active}
                onClick={() =>
                  setAllergies((current) =>
                    current.includes(option) ? current.filter((item) => item !== option) : [...current, option],
                  )
                }
                className={cn(
                  "inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3.5 py-2 text-xs font-bold transition-all",
                  active
                    ? "border-teal bg-teal-soft text-teal-strong"
                    : "border-line bg-paper text-ink-soft hover:border-line-strong hover:text-ink",
                )}
              >
                <span
                  className={cn(
                    "grid size-4 place-items-center rounded-full border",
                    active ? "border-teal bg-teal text-white" : "border-line-strong text-transparent",
                  )}
                  aria-hidden="true"
                >
                  <Check size={10} strokeWidth={3} />
                </span>
                {option}
              </button>
            );
          })}
        </div>
      </fieldset>

      {allergies.includes(OTHER_ALLERGY) && (
        <div className="animate-rise rounded-xl border border-line bg-cloud p-3.5">
          <Field
            label="Especificá la alergia"
            name="allergyOther"
            value={allergyOther}
            onChange={(event) => setAllergyOther(event.target.value)}
            placeholder="Ej. Penicilina, kiwi"
            hint="Si hay más de una, separalas con comas (,). Ej. Penicilina, kiwi"
          />
        </div>
      )}

      <Field
        as="textarea"
        label={
          <span className="inline-flex items-center gap-1.5">
            <Sparkles size={13} className="text-teal-strong" aria-hidden="true" /> Preferencias
          </span>
        }
        name="preferences"
        value={health.preferences}
        onChange={setHealthField("preferences")}
        placeholder="Ej. Desayuno temprano, cuna para bebé, piso alto…"
      />

      {error && <Alert>{error}</Alert>}
    </div>
  );
}

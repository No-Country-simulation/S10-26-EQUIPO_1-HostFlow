// Progreso del onboarding: etiqueta del paso + barra segmentada con la marca.
// Queda fijo arriba mientras se completa el formulario: el título del paso
// sigue visible aun al llegar al final de un formulario largo.
export default function OnboardingProgress({ steps, step }) {
  const CurrentIcon = steps[step].icon;

  return (
    <div className="sticky top-[75px] z-20 -mx-4 -mt-6 grid gap-2 border-b border-line bg-paper/95 px-4 pb-3 pt-4 backdrop-blur sm:-mx-6 sm:px-6">
      <div className="flex items-center justify-between gap-3 text-[11px] font-bold">
        <span className="inline-flex min-w-0 items-center gap-1.5 text-teal-strong">
          <CurrentIcon size={13} aria-hidden="true" />
          <span className="truncate">{steps[step].label}</span>
        </span>
        <span className="shrink-0 text-muted" aria-hidden="true">
          Paso {step + 1} de {steps.length}
        </span>
      </div>
      <div
        className="flex gap-1.5"
        role="progressbar"
        aria-label="Progreso de la ficha de llegada"
        aria-valuemin={1}
        aria-valuemax={steps.length}
        aria-valuenow={step + 1}
        aria-valuetext={`Paso ${step + 1} de ${steps.length}: ${steps[step].label}`}
      >
        {steps.map((item, index) => (
          <span
            key={item.id}
            className="h-2 flex-1 rounded-full transition-colors duration-300"
            style={{
              backgroundColor: index <= step ? "var(--hf-hero)" : "var(--color-overlay, rgba(17, 17, 17, 0.06))",
            }}
          />
        ))}
      </div>
    </div>
  );
}

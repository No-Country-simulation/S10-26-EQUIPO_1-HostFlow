// Encabezado de cada paso: ícono en color de marca + etiqueta + título. Es
// presentación (no cambia el flujo) y hereda la identidad del alojamiento.
export default function StepHeading({ icon: Icon, eyebrow, title, text, aside }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div className="flex min-w-0 items-start gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-[var(--radius-chip)] bg-teal-soft text-teal-strong">
          <Icon size={19} aria-hidden="true" />
        </span>
        <div className="min-w-0">
          <p className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-teal-strong">{eyebrow}</p>
          <h1 className="mt-0.5 text-xl font-extrabold text-ink">{title}</h1>
          {text && <p className="mt-1 text-sm text-muted">{text}</p>}
        </div>
      </div>
      {aside}
    </div>
  );
}

import { Sparkles } from "lucide-react";

// ── Marquee ─────────────────────────────────────────────────────────────────

export function LogoMarquee() {
  const names = ["Cabañas del Río", "Posada Delta", "Albergue La Lancha", "Hotel Tigre", "Lo del Mono", "Mirador del Paraná", "Casa Flotante"];
  // Dos grupos IDÉNTICOS: el keyframe mueve el track un -50 %, que equivale
  // exactamente al ancho de un grupo. Así el bucle cierra sin saltos (antes el
  // `gap` y el `pr` de la lista duplicada no coincidían y la animación “cortaba”
  // al reiniciar).
  const groups = [0, 1];

  return (
    <section className="relative overflow-hidden border-y border-line bg-cloud py-8 sm:py-9">
      {/* Trama diagonal fina: da textura sin competir con las etiquetas. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 opacity-[0.1]"
        style={{
          backgroundImage:
            "repeating-linear-gradient(-45deg, var(--color-ink) 0 1px, transparent 1px 9px)",
        }}
      />
      {/* Manchas de marca apenas visibles: solo un halo de color en los bordes,
          sin el degradado fuerte que dominaba la sección. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -left-28 top-1/2 size-40 -translate-y-1/2 rounded-full bg-lime/10 blur-3xl"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-28 top-1/2 size-40 -translate-y-1/2 rounded-full bg-teal/10 blur-3xl"
      />

      <div className="relative mx-auto max-w-6xl px-5 sm:px-6">
        {/* Rótulo centrado con punto de marca: el recurso de eyebrow del home,
            sin las reglas largas a los costados. */}
        <p className="mb-5 flex items-center justify-center gap-2 text-[11px] font-extrabold uppercase tracking-[0.18em] text-muted">
          <span aria-hidden="true" className="size-1.5 rounded-full bg-lime" />
          Confían en HostFlow
        </p>

        <div className="group relative overflow-hidden [mask-image:linear-gradient(90deg,transparent,black_8%,black_92%,transparent)]">
          <div className="flex w-max animate-marquee group-hover:[animation-play-state:paused] motion-reduce:animate-none">
            {groups.map((group) => (
              <ul
                key={group}
                aria-hidden={group === 1 ? "true" : undefined}
                className="flex shrink-0 items-center gap-8 pr-8"
              >
                {names.map((name) => (
                  <li
                    key={`${group}-${name}`}
                    className="flex shrink-0 items-center gap-2 whitespace-nowrap"
                  >
                    <Sparkles size={14} className="text-teal-strong" aria-hidden="true" />
                    <span className="text-[13px] font-bold text-ink">{name}</span>
                  </li>
                ))}
              </ul>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

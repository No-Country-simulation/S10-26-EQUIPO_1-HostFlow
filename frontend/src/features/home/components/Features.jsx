import { cn } from "@utils/format";
import { Reveal } from "./Reveal";
import { features } from "../lib/features";

// ── Features ────────────────────────────────────────────────────────────────

export function Features() {
  const tones = { lime: "bg-lime text-lime-ink", teal: "bg-teal-soft text-teal-strong", pink: "bg-pink-soft text-pink-strong" };
  return (
    <section id="caracteristicas" className="mx-auto max-w-6xl px-5 py-20 sm:px-6 lg:py-28">
      <Reveal className="max-w-2xl">
        <span className="mb-3 inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-['']">
          Características
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
          Todo lo que necesitás, <span className="text-gradient-lime">nada que sobre</span>
        </h2>
        <p className="mt-4 text-lg leading-relaxed text-muted">
          Herramientas pensadas para pequeños y medianos alojamientos que quieren crecer sin complicarse.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {features.map((feature, index) => (
          <Reveal key={feature.title} delay={index * 90}>
            <article className="card card-hover group relative h-full overflow-hidden p-6">
              <span
                aria-hidden="true"
                className="absolute -right-4 -top-5 font-display text-[86px] font-semibold leading-none text-ink/[0.05] transition-transform duration-500 group-hover:-translate-y-1 group-hover:rotate-6"
              >
                {index + 1}
              </span>
              <span className={cn("relative mb-5 grid size-12 place-items-center rounded-2xl transition-transform duration-300 group-hover:-translate-y-1 group-hover:scale-110", tones[feature.tone])}>
                <feature.icon size={22} />
              </span>
              <h3 className="relative mb-2 font-bold text-ink">{feature.title}</h3>
              <p className="relative text-sm leading-relaxed text-muted">{feature.text}</p>
            </article>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

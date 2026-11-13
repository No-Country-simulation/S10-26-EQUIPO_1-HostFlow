import { cn } from "@utils/format";
import { Reveal } from "./Reveal";
import { SceneLancha } from "./SceneLancha";
import { SceneInterior } from "./SceneInterior";
import { SceneDesayuno } from "./SceneDesayuno";

// ── Gallery: "Así se vive" ──────────────────────────────────────────────────
// Tres láminas ilustradas en formato polaroid, con cinta y rotación: un bloque
// editorial que rompe la grilla perfecta típica de las landing generadas.

export function Gallery() {
  const cards = [
    {
      art: <SceneLancha />,
      title: "La llegada en lancha",
      text: "El huésped desembarca en tu muelle con su ficha ya cargada.",
      tilt: "-rotate-2",
      delay: 0,
    },
    {
      art: <SceneInterior />,
      title: "La casa lista",
      text: "Fechas bloqueadas, reservas confirmadas y todo al día.",
      tilt: "rotate-1",
      delay: 110,
    },
    {
      art: <SceneDesayuno />,
      title: "Los adicionales",
      text: "Desayunos y excursiones que el huésped suma desde su enlace.",
      tilt: "rotate-3",
      delay: 220,
    },
  ];

  return (
    <section className="mx-auto max-w-6xl px-5 py-20 sm:px-6 lg:py-24">
      <Reveal className="max-w-2xl">
        <span className="mb-3 inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-['']">
          Así se vive
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
          Del desembarco al <span className="font-display italic">desayuno</span>, todo en un lugar
        </h2>
        <p className="mt-4 text-lg leading-relaxed text-muted">
          Tres momentos chicos del día a día de un alojamiento, resueltos en la misma plataforma.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-8 sm:grid-cols-2 lg:grid-cols-3 lg:gap-6">
        {cards.map((card, index) => (
          <Reveal key={card.title} delay={card.delay}>
            <figure
              className={cn(
                "tape group relative rotate-0 rounded-[22px] border border-line bg-paper p-3 pb-5 shadow-card transition-all duration-300",
                card.tilt,
                "hover:rotate-0 hover:-translate-y-1.5 hover:shadow-pop",
              )}
              style={{ animationDelay: `${index * 60}ms` }}
            >
              <div className="grain relative overflow-hidden rounded-[16px] border border-line">
                {card.art}
              </div>
              <figcaption className="px-1.5 pt-4">
                <p className="font-display text-lg font-semibold text-ink">{card.title}</p>
                <p className="mt-1 text-sm leading-relaxed text-muted">{card.text}</p>
              </figcaption>
            </figure>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

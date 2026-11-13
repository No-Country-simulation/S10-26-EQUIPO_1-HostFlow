import { Reveal } from "./Reveal";

// ── Stats ───────────────────────────────────────────────────────────────────

export function Stats() {
  const stats = [
    { value: "50+", label: "Alojamientos activos" },
    { value: "1.2k", label: "Huéspedes gestionados" },
    { value: "98%", label: "Satisfacción" },
    { value: "24/7", label: "Soporte" },
  ];
  return (
    <section className="mx-auto max-w-6xl px-5 py-16 sm:px-6">
      <Reveal className="relative overflow-hidden rounded-[28px] border border-line bg-gradient-to-r from-lime-soft via-teal-soft to-pink-soft px-5 py-10 sm:px-8 sm:py-12">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-30 [mask-image:radial-gradient(ellipse_at_left,black,transparent_70%)]"
          style={{
            backgroundImage:
              "repeating-linear-gradient(-45deg, var(--color-ink) 0 1px, transparent 1px 10px)",
          }}
        />
        <div className="relative grid grid-cols-2 gap-6 md:grid-cols-4 sm:gap-8">
          {stats.map((stat, index) => (
            <div key={stat.label} className="text-center" style={{ animationDelay: `${index * 70}ms` }}>
              <p className="text-[clamp(30px,4vw,44px)] font-extrabold leading-none text-ink">{stat.value}</p>
              <p className="mt-2 text-sm font-semibold text-ink-soft">{stat.label}</p>
            </div>
          ))}
        </div>
      </Reveal>
    </section>
  );
}

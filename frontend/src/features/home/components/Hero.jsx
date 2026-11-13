import { Link } from "react-router";
import { ArrowRight, CalendarCheck, MapPin, Star } from "lucide-react";
import { buttonClass } from "@components/ui";
import { cn } from "@utils/format";
import { Reveal } from "./Reveal";
import { DeltaScene } from "./DeltaScene";

// ── Hero ────────────────────────────────────────────────────────────────────

export function Hero() {
  return (
    <section className="relative overflow-hidden px-5 pt-14 pb-16 sm:px-6 lg:pt-20 lg:pb-24">
      <div className="pointer-events-none absolute inset-0 -z-10" aria-hidden="true">
        <div className="absolute -top-28 right-[-8%] size-[520px] rounded-full bg-lime/25 blur-[140px]" />
        <div className="absolute bottom-[-32%] left-[-12%] size-[440px] rounded-full bg-teal/15 blur-[140px]" />
        <div className="absolute inset-0 dot-grid opacity-35 [mask-image:radial-gradient(ellipse_at_center,black,transparent_72%)]" />
      </div>

      <div className="mx-auto max-w-6xl">
        <div className="grid items-center gap-12 lg:grid-cols-[1.02fr_0.98fr] lg:gap-8">
          <div className="animate-rise">
            <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-line bg-paper px-4 py-1.5 text-xs font-bold text-ink-soft shadow-card">
              <span className="relative flex size-2">
                <span className="absolute inline-flex size-full animate-ping rounded-full bg-teal opacity-60" />
                <span className="relative inline-flex size-2 rounded-full bg-teal" />
              </span>
              Nuevo · Gestión de alojamientos del Delta
            </span>

            <h1 className="text-[clamp(38px,6.4vw,68px)] font-extrabold leading-[1.02] tracking-[-0.035em]">
              Recibí a tus huéspedes con{" "}
              <span className="text-gradient-lime underline-hand">fluidez</span>
            </h1>

            <p className="mt-6 max-w-lg text-lg leading-relaxed text-muted">
              HostFlow centraliza reservas, adicionales y disponibilidad en una plataforma
              con carácter, pensada para alojamientos que quieren profesionalizar su gestión
              sin perder la calidez de siempre.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-4">
              <Link to="/auth/registro" className={buttonClass("primary", "min-h-[54px] px-7 text-[15px]")}>
                Comenzar gratis <ArrowRight size={18} />
              </Link>
              <a href="#como-funciona" className="group inline-flex items-center gap-2 text-sm font-bold text-ink-soft transition-colors hover:text-ink">
                Ver cómo funciona
                <ArrowRight size={15} className="transition-transform group-hover:translate-x-1" />
              </a>
            </div>

            <div className="mt-10 flex items-center gap-4">
              <div className="flex -space-x-2.5">
                {["M", "L", "C", "J"].map((initial, index) => (
                  <span
                    key={initial}
                    className={cn(
                      "grid size-10 place-items-center rounded-full border-2 border-paper text-xs font-extrabold shadow-sm",
                      index % 2 ? "bg-teal-soft text-teal-strong" : "bg-lime text-lime-ink",
                    )}
                  >
                    {initial}
                  </span>
                ))}
              </div>
              <div>
                <div className="flex items-center gap-1">
                  {[...Array(5)].map((_, i) => (
                    <Star key={i} size={15} className="fill-ink text-ink" />
                  ))}
                  <span className="ml-1 text-sm font-extrabold">4.9</span>
                </div>
                <p className="text-xs text-muted">+50 anfitriones confían en HostFlow</p>
              </div>
            </div>
          </div>

          {/* Escena ilustrada del Delta: dibujo propio, no stock ni render. */}
          <Reveal className="relative animate-rise" delay={120}>
            <div className="grain relative overflow-hidden rounded-[30px] border border-line shadow-pop">
              <DeltaScene />

              {/* Stickers sobre la escena */}
              <span
                className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-paper/90 px-3 py-1.5 text-[11px] font-extrabold text-ink shadow-card backdrop-blur animate-drift"
                style={{ "--drift-rotate": "-2deg" }}
              >
                <MapPin size={13} className="text-teal-strong" /> Delta del Tigre
              </span>

              <div
                className="absolute bottom-4 left-4 right-4 flex items-center gap-3 rounded-2xl border border-line bg-paper/92 p-3 shadow-pop backdrop-blur-sm sm:left-auto sm:w-[264px]"
                style={{ animationDelay: "600ms" }}
              >
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-lime text-lime-ink animate-flicker">
                  <CalendarCheck size={18} />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-extrabold text-ink">María G. · 3 huéspedes</p>
                  <p className="truncate text-xs text-muted">Check-in mañana · 14:00 hs</p>
                </div>
                <span className="ml-auto shrink-0 rounded-full bg-lime-soft px-2 py-1 text-[10px] font-extrabold text-lime-ink">
                  Confirmado
                </span>
              </div>
            </div>

            <span
              className="absolute -right-3 -top-4 hidden rotate-6 rounded-full bg-pink px-3.5 py-1.5 text-[11px] font-extrabold text-ink shadow-card sm:block animate-drift"
              style={{ "--drift-rotate": "6deg", animationDelay: "900ms" }}
            >
              sin comisiones
            </span>
          </Reveal>
        </div>
      </div>
    </section>
  );
}

import { Coffee, Quote, Star } from "lucide-react";
import { Reveal } from "./Reveal";

// ── Testimonio ──────────────────────────────────────────────────────────────

export function Testimonial() {
  return (
    <section className="mx-auto max-w-6xl px-5 py-20 sm:px-6 lg:py-24">
      <Reveal className="grid items-center gap-10 lg:grid-cols-[1.4fr_1fr]">
        <figure className="relative rounded-[28px] border border-line bg-cloud p-7 sm:p-10">
          <Quote size={40} className="mb-4 text-lime-strong" aria-hidden="true" />
          <blockquote className="font-display text-[clamp(20px,2.6vw,30px)] font-medium italic leading-[1.35] text-ink">
            “Antes manejaba todo por WhatsApp y se me cruzaban los días. Con HostFlow las reservas
            están en un calendario, los huéspedes completan su ficha antes de llegar y yo duermo
            tranquila.”
          </blockquote>
          <figcaption className="mt-7 flex items-center gap-3.5">
            <span className="grid size-12 shrink-0 place-items-center rounded-full bg-teal text-sm font-extrabold text-white shadow-card">
              CM
            </span>
            <div>
              <p className="font-bold text-ink">Carolina Méndez</p>
              <p className="text-sm text-muted">Propietaria · Posada Delta, Tigre</p>
            </div>
            <span className="ml-auto hidden gap-1 sm:flex" aria-label="5 de 5 estrellas">
              {[...Array(5)].map((_, i) => (
                <Star key={i} size={15} className="fill-ink text-ink" />
              ))}
            </span>
          </figcaption>
        </figure>

        <div className="relative mx-auto w-full max-w-[300px] rotate-2 transition-transform duration-300 hover:rotate-0">
          <div className="tape grain relative overflow-hidden rounded-[20px] border border-line bg-paper p-3 pb-14 shadow-pop">
            <div className="grid aspect-[4/5] place-items-center rounded-[14px] bg-gradient-to-br from-teal-soft via-lime-soft to-pink-soft">
              <div className="grid place-items-center gap-2 text-center">
                <span className="grid size-20 place-items-center rounded-full bg-paper font-display text-3xl font-semibold text-ink shadow-card">
                  <Coffee size={30} aria-hidden="true" />
                </span>
                <p className="max-w-[180px] px-3 text-xs font-semibold text-ink-soft">
                  6:40 · ya está el café para la llegada de las 7
                </p>
              </div>
            </div>
            <p className="absolute inset-x-4 bottom-4 text-center font-display text-sm italic text-muted">
              — del cuaderno de una anfitriona
            </p>
          </div>
        </div>
      </Reveal>
    </section>
  );
}

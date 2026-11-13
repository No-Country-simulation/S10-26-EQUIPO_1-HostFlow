import { Link } from "react-router";
import { ArrowRight } from "lucide-react";
import { buttonClass } from "@components/ui";
import { Reveal } from "./Reveal";
import { steps } from "../lib/steps";

// ── How it works ────────────────────────────────────────────────────────────

export function HowItWorks() {
  return (
    <section id="como-funciona" className="relative overflow-hidden border-y border-line bg-cloud py-20 lg:py-28">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 opacity-[0.07]"
        style={{
          backgroundImage: "radial-gradient(var(--color-ink) 1.2px, transparent 1.2px)",
          backgroundSize: "26px 26px",
        }}
      />
      <div className="relative mx-auto max-w-6xl px-5 sm:px-6">
        <Reveal className="mx-auto max-w-2xl text-center">
          <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
            Cómo funciona
          </span>
          <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">Simple desde el primer día</h2>
        </Reveal>

        <div className="mt-16 grid gap-10 md:grid-cols-3 md:gap-8">
          {steps.map((step, index) => (
            <Reveal key={step.title} delay={index * 120} className="relative text-center">
              {index < steps.length - 1 && (
                <span
                  aria-hidden="true"
                  className="absolute left-[calc(50%+46px)] top-10 hidden h-px w-[calc(100%-92px)] border-t-2 border-dashed border-line-strong md:block"
                />
              )}
              <span className="relative mx-auto mb-6 grid size-20 place-items-center rounded-3xl border border-line bg-paper text-2xl font-extrabold text-ink shadow-card">
                <span className="font-display italic">{index + 1}</span>
                <span className="absolute -right-1.5 -top-1.5 grid size-6 place-items-center rounded-full bg-lime text-[11px] font-extrabold text-lime-ink">
                  ✓
                </span>
              </span>
              <h3 className="mb-2 text-lg font-bold text-ink">{step.title}</h3>
              <p className="mx-auto max-w-xs text-sm leading-relaxed text-muted">{step.text}</p>
            </Reveal>
          ))}
        </div>

        <Reveal className="mt-14 flex justify-center" delay={120}>
          <Link to="/auth/registro" className={buttonClass("primary", "min-h-[52px] px-7")}>
            Empezar ahora <ArrowRight size={18} />
          </Link>
        </Reveal>
      </div>
    </section>
  );
}

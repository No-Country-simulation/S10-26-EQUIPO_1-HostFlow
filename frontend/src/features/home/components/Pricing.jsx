import { Link } from "react-router";
import { ArrowRight, Check } from "lucide-react";
import { buttonClass } from "@components/ui";
import { cn } from "@utils/format";
import { Reveal } from "./Reveal";
import { plans } from "../lib/plans";

// ── Pricing ─────────────────────────────────────────────────────────────────

export function Pricing() {
  return (
    <section id="precios" className="border-y border-line bg-cloud py-20 lg:py-28">
      <div className="mx-auto max-w-5xl px-5 sm:px-6">
        <Reveal className="mx-auto max-w-2xl text-center">
          <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
            Precios
          </span>
          <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
            Todas las características incluidas
          </h2>
          <p className="mt-4 text-lg text-muted">Sin compromiso. Cancelá cuando quieras.</p>
        </Reveal>

        <div className="mt-14 grid gap-6 md:grid-cols-2">
          {plans.map((plan, index) => (
            <Reveal key={plan.name} delay={index * 110}>
              <article
                className={cn(
                  "card relative h-full overflow-hidden p-6 sm:p-8",
                  plan.highlight && "border-lime ring-2 ring-lime/40",
                )}
              >
                {plan.highlight && (
                  <span className="absolute right-5 top-5 rounded-full bg-lime px-3 py-1 text-[11px] font-extrabold text-lime-ink">
                    Recomendado
                  </span>
                )}
                <h3 className="font-extrabold text-ink">{plan.name}</h3>
                <p className="mt-1 text-sm text-muted">{plan.tagline}</p>
                <div className="mt-5 flex items-end gap-1">
                  <span className="text-[clamp(36px,5vw,52px)] font-extrabold leading-none tracking-tight text-ink">{plan.price}</span>
                  {plan.period && <span className="pb-1 text-sm font-semibold text-muted">{plan.period}</span>}
                </div>
                <ul className="mt-7 grid gap-3">
                  {plan.features.map((feature) => (
                    <li key={feature} className="flex items-start gap-2.5 text-sm text-ink-soft">
                      <Check size={16} className="mt-0.5 shrink-0 text-teal-strong" />
                      {feature}
                    </li>
                  ))}
                </ul>
                <Link
                  to="/auth/registro"
                  className={buttonClass(plan.highlight ? "primary" : "secondary", "mt-8 w-full")}
                >
                  {plan.cta} <ArrowRight size={17} />
                </Link>
              </article>
            </Reveal>
          ))}
        </div>

        <p className="mt-8 text-center text-xs text-muted">
          +$20 por usuario adicional al mes, más impuestos. *Procesamiento de pagos no incluido.
        </p>
      </div>
    </section>
  );
}

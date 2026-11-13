import { Link } from "react-router";
import { ArrowRight, Palette } from "lucide-react";
import { buttonClass } from "@components/ui";
import { Reveal } from "./Reveal";

// ── CTA final ───────────────────────────────────────────────────────────────

export function FinalCTA() {
  return (
    <section className="mx-auto max-w-6xl px-5 pb-20 sm:px-6">
      {/* Card "inversa" de firma: usa neutros FIJOS (#101518 / #f2f5f4) en vez
          de los tokens del tema, porque `ink`/`paper` se invierten en modo oscuro
          y la card quedaba clara sobre fondo oscuro (al revés del tema). */}
      <Reveal className="relative overflow-hidden rounded-[28px] bg-[#101518] px-6 py-16 text-center text-[#f2f5f4] sm:px-12">
        <div className="absolute inset-0 [background-image:radial-gradient(rgba(242,245,244,0.14)_1px,transparent_1px)] opacity-100 [mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]" />
        <div
          aria-hidden="true"
          className="absolute -left-10 -top-12 size-52 rounded-full bg-lime/20 blur-3xl animate-drift-slow"
        />
        <div
          aria-hidden="true"
          className="absolute -bottom-16 -right-8 size-56 rounded-full bg-teal/25 blur-3xl animate-drift-slow"
          style={{ animationDelay: "2s" }}
        />
        <span
          aria-hidden="true"
          className="absolute left-6 top-6 hidden rotate-[-8deg] rounded-full border border-[#f2f5f4]/25 px-3 py-1 text-[11px] font-extrabold uppercase tracking-wider text-[#f2f5f4]/75 sm:block animate-drift"
          style={{ "--drift-rotate": "-8deg" }}
        >
          sin tarjeta
        </span>
        <div className="relative mx-auto max-w-2xl">
          <Palette size={32} className="mx-auto mb-5 text-lime" aria-hidden="true" />
          <h2 className="text-[clamp(26px,4vw,42px)] font-extrabold leading-tight">
            Empezá a recibir huéspedes hoy
          </h2>
          <p className="mx-auto mt-4 max-w-md text-[#f2f5f4]/70">
            Creá tu cuenta, personalizá tu alojamiento y compartí el enlace. Sin tarjeta.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link to="/auth/registro" className={buttonClass("primary", "min-h-[52px] px-7")}>
              Crear cuenta gratis <ArrowRight size={18} />
            </Link>
            <Link to="/auth/login" className="inline-flex min-h-[52px] items-center rounded-xl border border-[#f2f5f4]/25 px-6 text-sm font-bold text-[#f2f5f4] transition hover:bg-[#f2f5f4]/10">
              Ya tengo cuenta
            </Link>
          </div>
        </div>
      </Reveal>
    </section>
  );
}

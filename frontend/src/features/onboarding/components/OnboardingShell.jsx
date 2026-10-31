import { Link } from "react-router";
import { BrandLogo, LogoMark, ThemeToggle, DemoBanner } from "@components/ui";
import { LodgingTheme } from "@lib/theme/LodgingTheme";

// Shell de las páginas PÚBLICAS del huésped (onboarding B2C).
// Aplica la identidad del alojamiento: banda de marca con el color principal y
// superficie teñida con el color de fondo + patrón. Las fotos del alojamiento
// se presentan en la portada (carrusel) y después una por paso del formulario.
// `wide` da más ancho a la portada; el formulario queda en una columna legible.
export default function OnboardingShell({ lodging, children, wide = false }) {
  const width = wide ? "max-w-4xl" : "max-w-2xl";

  return (
    <LodgingTheme brand={lodging?.brand} className="min-h-dvh text-ink">
      <div className="hf-surface flex min-h-dvh flex-col">
        <DemoBanner />

        {/* Cabecera de marca: color plano del alojamiento (sin degradé) */}
        <header
          className="sticky top-0 z-30 border-b-[3px] px-4 py-3.5 shadow-card sm:px-6"
          style={{
            backgroundColor: "var(--hf-hero)",
            color: "var(--hf-hero-ink)",
            borderColor: "var(--color-lime)",
          }}
        >
          <div className={`mx-auto flex w-full items-center gap-3 ${width}`}>
            <BrandLogo src={lodging?.logo} name={lodging?.name} size="md" />
            <div className="min-w-0 flex-1">
              <p className="truncate font-display text-lg font-extrabold leading-tight">
                {lodging?.name || "Alojamiento"}
              </p>
              <p className="truncate text-[11px] font-semibold opacity-80">
                {lodging?.tagline || "Llegada de huéspedes"}
              </p>
            </div>
            <ThemeToggle />
          </div>
        </header>

        <main className={`mx-auto w-full flex-1 px-4 py-6 sm:px-6 ${width}`}>
          <div className="animate-rise">{children}</div>
        </main>

        <footer className="border-t border-line px-4 py-5 text-center text-xs text-muted sm:px-6">
          <span className="inline-flex items-center gap-1.5">
            <LogoMark className="size-4" /> HostFlow ·{" "}
            <Link to="/" className="font-semibold text-teal-strong transition-colors hover:text-teal">
              Conocé más
            </Link>
          </span>
        </footer>
      </div>
    </LodgingTheme>
  );
}

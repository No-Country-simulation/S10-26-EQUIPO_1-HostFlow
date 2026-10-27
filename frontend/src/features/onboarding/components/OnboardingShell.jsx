import { Link } from "react-router";
import { LogoMark, ThemeToggle, DemoBanner } from "@components/ui";
import { LodgingTheme } from "@lib/theme/LodgingTheme";

// Shell de las páginas PÚBLICAS del huésped (onboarding B2C).
// Aplica la identidad del alojamiento (logo + colores personalizados) con el
// sistema de temas existente (LodgingTheme), sin cuenta ni sesión.
export default function OnboardingShell({ lodging, children }) {
  return (
    <LodgingTheme
      brand={lodging?.brand}
      className="flex min-h-dvh flex-col bg-cloud text-ink"
    >
      <DemoBanner />

      <header className="sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-line bg-paper/85 px-4 py-3 backdrop-blur-md sm:px-6">
        <div className="flex min-w-0 items-center gap-3">
          {lodging?.logo ? (
            <img src={lodging.logo} alt={`Logo de ${lodging.name}`} className="size-10 rounded-xl bg-white object-contain p-1" />
          ) : (
            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-teal text-white">
              <LogoMark className="size-7" />
            </span>
          )}
          <div className="min-w-0">
            <p className="truncate font-extrabold text-ink">{lodging?.name || "Alojamiento"}</p>
            <p className="truncate text-xs text-muted">{lodging?.tagline || "Llegada de huéspedes"}</p>
          </div>
        </div>
        <ThemeToggle />
      </header>

      <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8 sm:px-6">
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
    </LodgingTheme>
  );
}

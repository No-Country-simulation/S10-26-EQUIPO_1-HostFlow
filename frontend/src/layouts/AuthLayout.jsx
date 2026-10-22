import { Outlet } from "react-router";
import { Brand, DemoBanner, ThemeToggle } from "../components/ui";

export default function AuthLayout() {
  return (
    <div className="flex min-h-dvh flex-col bg-paper text-ink">
      <DemoBanner />

      <div className="relative grid flex-1 place-items-center overflow-hidden px-4 py-10">
        {/* Decoración */}
        <div className="pointer-events-none absolute inset-0 -z-10" aria-hidden="true">
          <div className="absolute -top-32 -left-24 size-[420px] rounded-full bg-lime/40 blur-[120px]" />
          <div className="absolute -bottom-40 -right-24 size-[460px] rounded-full bg-teal/20 blur-[130px]" />
          <div className="absolute inset-0 dot-grid opacity-[0.35] [mask-image:radial-gradient(ellipse_at_center,black,transparent_70%)]" />
        </div>

        <div className="absolute inset-x-0 top-0 flex items-center justify-between px-5 py-4 sm:px-8">
          <Brand />
          <ThemeToggle />
        </div>

        <main className="relative z-0 w-full max-w-[460px] animate-rise pt-16">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

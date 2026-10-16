import { Waves } from "lucide-react";
import { Outlet } from "react-router";
import { Brand, DemoBanner, Eyebrow } from "../components/ui";

export default function AuthLayout() {
  return (
    <div className="grid min-h-dvh grid-cols-[minmax(400px,0.95fr)_1.25fr] bg-paper max-[800px]:block">
      <aside className="relative isolate m-3.5 flex min-h-[calc(100vh-28px)] flex-col overflow-hidden rounded-[26px] bg-story px-13 py-11 text-white shadow-md before:absolute before:inset-0 before:-z-10 before:bg-wave before:bg-size-[140px_28px] before:content-[''] before:mask-[linear-gradient(180deg,transparent_10%,#000_90%)] max-[800px]:hidden">
        <Brand light />

        <div className="relative z-1 mb-15 mt-auto max-w-140">
          <Eyebrow light>Gestión simple. Hospitalidad real.</Eyebrow>

          <h1 className="mb-6 text-[clamp(36px,4.2vw,62px)] font-medium leading-[1.06] tracking-[-0.03em]">
            Tu alojamiento,
            <br />
            más cerca de cada huésped.
          </h1>

          <p className="max-w-120 text-[17.5px] leading-[1.7] text-[#d4e6df]">
            Centralizá el check-in, los adicionales y los traslados en una experiencia pensada para
            el Delta.
          </p>
        </div>

        <Waves className="absolute -bottom-32.5 -right-30 -z-10 size-110 opacity-[0.07]" />
      </aside>

      <main className="relative grid place-items-center overflow-auto px-[6%] py-16 max-[800px]:min-h-screen max-[800px]:px-5.5 max-[800px]:pb-10 max-[800px]:pt-20.5">
        <DemoBanner className="absolute inset-x-0 top-0" />
        <Outlet />
      </main>
    </div>
  );
}

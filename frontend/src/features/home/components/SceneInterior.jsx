import { SceneFrame } from "./SceneFrame";

export function SceneInterior() {
  return (
    <SceneFrame label="Ilustración del interior de una cabaña con ventana y mesa de noche">
      <rect width="400" height="300" fill="color-mix(in srgb, var(--color-cloud) 88%, var(--color-paper))" />
      {/* ventana con cortina */}
      <g stroke="var(--color-ink)" strokeWidth="3.5" strokeLinejoin="round">
        <rect x="42" y="40" width="150" height="128" rx="8" fill="color-mix(in srgb, var(--color-teal) 30%, var(--color-paper))" />
        <path d="M117 40v128M42 104h150" strokeWidth="2.6" />
        <path d="M42 40c26 14 34 74 0 128" fill="var(--color-paper)" opacity="0.65" stroke="none" />
      </g>
      {/* rayos de sol */}
      <path d="M196 62 340 128M196 118l146 46" stroke="var(--color-lime)" strokeWidth="14" strokeLinecap="round" opacity="0.5" className="animate-flicker" />
      {/* cama */}
      <g stroke="var(--color-ink)" strokeWidth="3.5" strokeLinejoin="round">
        <rect x="212" y="176" width="150" height="76" rx="10" fill="var(--color-paper)" />
        <path d="M212 204h150" strokeWidth="2.6" opacity="0.6" />
        <rect x="228" y="158" width="52" height="34" rx="10" fill="var(--color-pink)" />
        <rect x="292" y="164" width="46" height="28" rx="10" fill="var(--color-lime)" />
      </g>
      {/* mesa de noche + taza */}
      <g stroke="var(--color-ink)" strokeWidth="3.5" strokeLinejoin="round">
        <rect x="56" y="212" width="96" height="14" rx="5" fill="var(--color-paper)" />
        <path d="M70 226v46M138 226v46" />
        <path d="M84 186h30v22a10 10 0 0 1-10 10h-10a10 10 0 0 1-10-10Z" fill="var(--color-lime)" />
        <path d="M114 192h8a7 7 0 0 1 0 14h-8" fill="none" />
        <path d="M92 178c-4-6 4-10 0-16M106 178c-4-6 4-10 0-16" strokeWidth="2.4" opacity="0.6" className="animate-drift-slow" />
      </g>
      {/* planta */}
      <g stroke="var(--color-teal)" strokeWidth="4" fill="none" strokeLinecap="round" className="animate-sway" style={{ transformBox: "fill-box", transformOrigin: "bottom center" }}>
        <path d="M348 288c-2-30 4-48 16-62" />
        <path d="M348 288c2-24-6-40-18-50" opacity="0.8" />
      </g>
      <path d="M330 288h40l-6 30h-28Z" fill="var(--color-pink)" stroke="var(--color-ink)" strokeWidth="3.5" strokeLinejoin="round" transform="translate(0 -22)" />
    </SceneFrame>
  );
}

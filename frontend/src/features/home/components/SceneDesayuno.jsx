import { SceneFrame } from "./SceneFrame";

export function SceneDesayuno() {
  return (
    <SceneFrame label="Ilustración cenital de una mesa con desayuno: medialunas, café y fruta">
      <rect width="400" height="300" fill="color-mix(in srgb, var(--color-lime) 16%, var(--color-paper))" />
      <g stroke="var(--color-ink)" strokeWidth="3.5" strokeLinejoin="round">
        {/* mantel */}
        <rect x="30" y="26" width="340" height="248" rx="18" fill="var(--color-paper)" opacity="0.9" />
        {/* canasta de medialunas */}
        <ellipse cx="140" cy="120" rx="66" ry="46" fill="color-mix(in srgb, var(--color-lime) 40%, var(--color-paper))" />
        <path d="M86 118c14-24 40-34 62-28M112 140c16-22 42-30 62-22" strokeWidth="3" fill="none" opacity="0.7" />
        <ellipse cx="140" cy="120" rx="66" ry="46" fill="none" />
        {/* taza de café */}
        <g>
          <circle cx="284" cy="92" r="34" fill="color-mix(in srgb, var(--color-teal) 22%, var(--color-paper))" />
          <circle cx="284" cy="92" r="20" fill="color-mix(in srgb, var(--color-ink) 78%, var(--color-paper))" stroke="none" />
          <path d="M316 84h10a12 12 0 0 1 0 24h-10" fill="none" />
        </g>
        {/* plato con fruta */}
        <circle cx="120" cy="222" r="44" fill="var(--color-cloud)" />
        <circle cx="106" cy="216" r="15" fill="var(--color-pink)" />
        <circle cx="134" cy="228" r="13" fill="var(--color-lime)" />
        <circle cx="130" cy="204" r="11" fill="color-mix(in srgb, var(--color-teal) 60%, var(--color-lime))" />
        {/* servilleta + cubiertos */}
        <rect x="236" y="168" width="96" height="72" rx="10" fill="var(--color-cloud)" />
        <path d="M258 180v48M276 180v48" strokeWidth="3" opacity="0.65" />
        <path d="M310 180v20a8 8 0 0 1-8 8h-4" fill="none" strokeWidth="3" />
      </g>
      {/* vapor */}
      <path d="M276 66c-6-8 6-14 0-22M292 66c-6-8 6-14 0-22" stroke="var(--color-ink)" strokeWidth="3" fill="none" strokeLinecap="round" opacity="0.5" className="animate-drift-slow" />
    </SceneFrame>
  );
}

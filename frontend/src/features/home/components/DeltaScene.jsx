import { Tree } from "./Tree";

// Escena del río dibujada a mano en SVG: cielo, sol, arboleda, cabaña, muelle,
// lancha y agua en movimiento. Los colores salen de los tokens del tema, así
// funciona en modo claro y oscuro (y con la marca de un alojamiento).
export function DeltaScene() {
  const wave = "c26-9 54-9 80 0 s54 9 80 0";
  const waveLine = (y, times) =>
    `M-160 ${y} ` + Array.from({ length: times }, () => wave).join(" ");

  return (
    <svg
      viewBox="0 0 680 560"
      className="block h-auto w-full"
      role="img"
      aria-label="Ilustración de una cabaña de madera con muelle sobre el río del Delta del Tigre"
    >
      <defs>
        <linearGradient id="hf-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--color-cloud)" />
          <stop offset="100%" stopColor="color-mix(in srgb, var(--color-lime) 30%, var(--color-paper))" />
        </linearGradient>
        <linearGradient id="hf-river" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="color-mix(in srgb, var(--color-teal) 34%, var(--color-paper))" />
          <stop offset="100%" stopColor="color-mix(in srgb, var(--color-teal) 66%, var(--color-paper))" />
        </linearGradient>
        <clipPath id="hf-frame">
          <rect width="680" height="560" />
        </clipPath>
      </defs>

      <g clipPath="url(#hf-frame)">
        <rect width="680" height="560" fill="url(#hf-sky)" />

        {/* Sol */}
        <circle cx="552" cy="112" r="76" fill="var(--color-lime)" opacity="0.28" className="animate-flicker" />
        <circle cx="552" cy="112" r="46" fill="var(--color-lime)" className="animate-flicker" />

        {/* Aves */}
        <g
          stroke="var(--color-ink)"
          strokeWidth="3"
          fill="none"
          strokeLinecap="round"
          opacity="0.5"
          className="animate-drift-slow"
        >
          <path d="M126 104c9-10 18-10 27 0 9-10 18-10 27 0" />
          <path d="M206 74c6-7 13-7 19 0 6-7 13-7 19 0" opacity="0.7" />
        </g>

        {/* Colina lejana */}
        <path
          d="M0 318c64-44 126-56 186-36s112 22 172-8 132-28 190 6 96 42 132 28v92H0Z"
          fill="var(--color-teal)"
          opacity="0.16"
        />

        {/* Arboleda */}
        <Tree x={78} y={252} s={1.15} o={0.55} />
        <Tree x={322} y={274} s={0.85} o={0.4} />
        <Tree x={614} y={262} s={1} o={0.5} />
        <Tree x={468} y={286} s={0.7} o={0.32} />

        {/* Tierra */}
        <path d="M0 372c140-16 320-24 680-18v70H0Z" fill="color-mix(in srgb, var(--color-lime) 34%, var(--color-teal) 30%, var(--color-paper))" />
        <path
          d="M0 372c140-16 320-24 680-18"
          fill="none"
          stroke="var(--color-ink)"
          strokeWidth="3"
          opacity="0.75"
        />

        {/* Cabaña */}
        <g stroke="var(--color-ink)" strokeWidth="3.5" strokeLinejoin="round" strokeLinecap="round">
          <path d="M64 268 186 192l124 76Z" fill="var(--color-pink)" />
          <rect x="92" y="266" width="188" height="104" rx="5" fill="var(--color-paper)" />
          <rect x="118" y="300" width="52" height="70" rx="4" fill="var(--color-teal)" />
          <rect x="198" y="296" width="56" height="44" rx="4" fill="var(--color-lime)" className="animate-flicker" />
          <path d="M226 296v44M198 318h56" strokeWidth="2.4" />
          {/* Chimenea con humito */}
          <rect x="238" y="212" width="20" height="34" rx="3" fill="var(--color-cloud)" />
          <path
            d="M248 204c-8-8 8-14 0-24"
            fill="none"
            strokeWidth="3"
            opacity="0.55"
            className="animate-drift-slow"
          />
        </g>

        {/* Muelle */}
        <g stroke="var(--color-ink)" strokeWidth="3" strokeLinecap="round">
          <rect x="60" y="368" width="268" height="16" rx="4" fill="var(--color-paper)" />
          <path d="M124 368v16M188 368v16M252 368v16" strokeWidth="2.2" opacity="0.6" />
          <rect x="326" y="374" width="184" height="14" rx="4" fill="var(--color-cloud)" />
          <path d="M376 374v14M432 374v14M488 374v14" strokeWidth="2.2" opacity="0.6" />
          <path d="M348 388v46M492 388v46" />
        </g>

        {/* Agua */}
        <rect x="0" y="424" width="680" height="136" fill="url(#hf-river)" />
        <g
          stroke="var(--color-paper)"
          strokeWidth="4"
          fill="none"
          strokeLinecap="round"
          opacity="0.5"
          className="animate-water"
        >
          <path d={waveLine(452, 7)} />
          <path d={waveLine(496, 7)} opacity="0.72" />
          <path d={waveLine(538, 7)} opacity="0.5" />
        </g>

        {/* Reflejo de la cabaña */}
        <g stroke="var(--color-ink)" strokeWidth="3" opacity="0.18" strokeLinecap="round">
          <path d="M120 442h130M146 462h96M168 482h60" />
        </g>

        {/* Lancha */}
        <g className="animate-bob" style={{ transformBox: "fill-box", transformOrigin: "center" }}>
          <ellipse cx="452" cy="516" rx="86" ry="9" fill="var(--color-paper)" opacity="0.35" />
          <path
            d="M376 474h152l-20 32a14 14 0 0 1-11 6h-90a14 14 0 0 1-11-6Z"
            fill="var(--color-paper)"
            stroke="var(--color-ink)"
            strokeWidth="3.5"
            strokeLinejoin="round"
          />
          <rect x="404" y="450" width="60" height="24" rx="7" fill="var(--color-lime)" stroke="var(--color-ink)" strokeWidth="3.5" />
          <path d="M486 462h26" stroke="var(--color-ink)" strokeWidth="3.5" strokeLinecap="round" />
        </g>

        {/* Juncos */}
        <g
          stroke="var(--color-teal)"
          strokeWidth="5"
          strokeLinecap="round"
          fill="none"
          opacity="0.75"
          className="animate-sway"
          style={{ transformBox: "fill-box", transformOrigin: "bottom center" }}
        >
          <path d="M32 560c-4-40 6-64 18-84" />
          <path d="M58 560c2-34 14-52 30-66" opacity="0.8" />
          <path d="M12 560c-6-28-2-48 6-64" opacity="0.6" />
        </g>
        <g
          stroke="var(--color-teal)"
          strokeWidth="5"
          strokeLinecap="round"
          fill="none"
          opacity="0.7"
          className="animate-sway"
          style={{ transformBox: "fill-box", transformOrigin: "bottom center", animationDelay: "1.2s" }}
        >
          <path d="M654 560c6-38-2-62-14-82" />
          <path d="M628 560c2-32-14-50-28-64" opacity="0.75" />
        </g>
      </g>
    </svg>
  );
}

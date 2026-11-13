import { SceneFrame } from "./SceneFrame";

export function SceneLancha() {
  const wave = "c20-7 42-7 62 0 s42 7 62 0";
  const line = (y) => `M-140 ${y} ` + Array.from({ length: 6 }, () => wave).join(" ");
  return (
    <SceneFrame label="Ilustración de una lancha navegando por un canal del delta">
      <defs>
        <linearGradient id="g-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--color-cloud)" />
          <stop offset="100%" stopColor="color-mix(in srgb, var(--color-lime) 34%, var(--color-paper))" />
        </linearGradient>
      </defs>
      <rect width="400" height="300" fill="url(#g-sky)" />
      <circle cx="316" cy="66" r="34" fill="var(--color-lime)" className="animate-flicker" />
      <path d="M0 158c60-24 110-30 160-16s120 12 240-14v76H0Z" fill="var(--color-teal)" opacity="0.22" />
      <rect y="176" width="400" height="124" fill="color-mix(in srgb, var(--color-teal) 52%, var(--color-paper))" />
      <g stroke="var(--color-paper)" strokeWidth="3.5" fill="none" strokeLinecap="round" opacity="0.55" className="animate-water">
        <path d={line(212)} />
        <path d={line(252)} opacity="0.7" />
      </g>
      <g className="animate-bob" style={{ transformBox: "fill-box", transformOrigin: "center" }}>
        <path d="M126 186h148l-20 30a13 13 0 0 1-10 6h-88a13 13 0 0 1-10-6Z" fill="var(--color-paper)" stroke="var(--color-ink)" strokeWidth="3.5" strokeLinejoin="round" />
        <rect x="152" y="164" width="58" height="22" rx="7" fill="var(--color-lime)" stroke="var(--color-ink)" strokeWidth="3.5" />
      </g>
      <g stroke="var(--color-teal)" strokeWidth="5" strokeLinecap="round" fill="none" opacity="0.8" className="animate-sway" style={{ transformBox: "fill-box", transformOrigin: "bottom center" }}>
        <path d="M26 300c-4-40 4-66 16-88" />
        <path d="M52 300c2-32 12-52 28-68" opacity="0.75" />
        <path d="M374 300c4-38-4-62-16-84" />
        <path d="M348 300c-2-30-12-48-26-64" opacity="0.75" />
      </g>
    </SceneFrame>
  );
}

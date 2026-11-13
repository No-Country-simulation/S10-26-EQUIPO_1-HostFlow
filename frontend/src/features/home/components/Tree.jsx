export function Tree({ x, y, s = 1, o = 0.5 }) {
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`} opacity={o}>
      <rect x="-5" y="0" width="10" height="86" rx="4" fill="var(--color-ink)" opacity="0.75" />
      <ellipse cx="0" cy="-18" rx="46" ry="40" fill="var(--color-teal)" />
      <ellipse cx="-30" cy="6" rx="28" ry="24" fill="var(--color-teal)" opacity="0.9" />
      <ellipse cx="30" cy="4" rx="26" ry="22" fill="var(--color-teal)" opacity="0.9" />
    </g>
  );
}

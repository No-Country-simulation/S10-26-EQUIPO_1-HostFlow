export function SceneFrame({ children, label }) {
  return (
    <svg viewBox="0 0 400 300" className="block h-auto w-full" role="img" aria-label={label}>
      {children}
    </svg>
  );
}

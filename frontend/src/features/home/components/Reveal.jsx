import { useEffect, useRef, useState } from "react";
import { cn } from "@utils/format";

// ── Reveal (scroll) ─────────────────────────────────────────────────────────
// Aparece con un fundido + desplazamiento cuando la sección entra en pantalla.
// Si no hay IntersectionObserver (o el usuario prefiere menos movimiento) se
// muestra directamente: nunca deja contenido invisible.
export function Reveal({ as: Element = "div", delay = 0, className, children, ...props }) {
  const ref = useRef(null);
  const [shown, setShown] = useState(false);

  useEffect(() => {
    const node = ref.current;
    if (!node) return undefined;
    if (typeof IntersectionObserver === "undefined") {
      setShown(true);
      return undefined;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setShown(true);
            observer.disconnect();
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -48px 0px" },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  return (
    <Element
      ref={ref}
      style={{ transitionDelay: `${delay}ms` }}
      className={cn(
        "transition-[opacity,transform] duration-700 ease-out will-change-transform",
        shown ? "translate-y-0 opacity-100" : "translate-y-7 opacity-0",
        className,
      )}
      {...props}
    >
      {children}
    </Element>
  );
}

import { useEffect, useId, useRef } from "react";
import { X } from "lucide-react";
import { cn } from "@utils/format";

// Recuerda el último elemento enfocado FUERA de un diálogo (role="dialog").
// Hace falta porque un campo con `autoFocus` dentro del modal toma el foco
// ANTES de que corra el efecto del Modal: sin este rastreo, al cerrar se
// "restauraría" el foco al propio campo del modal en vez del botón de origen.
let lastFocusedOutsideDialog = null;
if (typeof document !== "undefined") {
  document.addEventListener("focusin", (event) => {
    if (!event.target?.closest?.('[role="dialog"]')) lastFocusedOutsideDialog = event.target;
  });
}

// Modal reutilizable con el sistema visual existente:
// backdrop difuminado, tarjeta `.card` y animación `pop` (globals.css).
// Accesible: role=dialog + aria-labelledby, foco inicial en el diálogo, trampa
// de foco con Tab/Shift+Tab, cierre con Escape y devolución del foco al abrirlo.
// Cierra también tocando el fondo; bloquea el scroll del body mientras está abierto.
export default function Modal({ open, onClose, title, subtitle, className, children, footer }) {
  const dialogRef = useRef(null);
  const previouslyFocused = useRef(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose; // siempre la última versión, sin re-efectos por render
  const titleId = useId();

  useEffect(() => {
    if (!open) return undefined;

    // Guarda quién tenía el foco (fuera del diálogo) y enfoca el diálogo al abrir
    // (sin pisar el foco si un campo con autoFocus ya lo tomó).
    previouslyFocused.current = lastFocusedOutsideDialog;
    const dialog = dialogRef.current;
    if (dialog && !dialog.contains(document.activeElement)) dialog.focus();

    const onKey = (event) => {
      if (event.key === "Escape") {
        onCloseRef.current?.();
        return;
      }
      if (event.key !== "Tab") return;

      // Trampa de foco: Tab solo circula entre los controles del modal.
      const focusables = dialogRef.current?.querySelectorAll(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );
      if (!focusables || focusables.length === 0) return;
      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      const active = document.activeElement;

      if (event.shiftKey && (active === first || active === dialogRef.current)) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", onKey);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = previousOverflow;
      // Devuelve el foco al elemento que abrió el modal.
      previouslyFocused.current?.focus?.();
    };
  }, [open]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[60] flex items-end justify-center sm:items-center">
      <div className="absolute inset-0 bg-black/45 backdrop-blur-sm animate-fade" onClick={() => onCloseRef.current?.()} />
      <div
        ref={dialogRef}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? titleId : undefined}
        aria-label={title ? undefined : "Diálogo"}
        className={cn(
          "relative z-10 max-h-[92dvh] w-full overflow-y-auto rounded-t-[26px] border border-line bg-paper p-5 shadow-pop animate-pop outline-none sm:max-w-lg sm:rounded-[26px] sm:p-6",
          className,
        )}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <div>
            {title && (
              <h2 id={titleId} className="break-words text-lg font-extrabold text-ink">
                {title}
              </h2>
            )}
            {subtitle && <p className="mt-1 break-words text-sm text-muted">{subtitle}</p>}
          </div>
          <button
            type="button"
            onClick={() => onCloseRef.current?.()}
            aria-label="Cerrar"
            className="grid size-9 shrink-0 cursor-pointer place-items-center rounded-lg text-ink-soft transition hover:bg-overlay hover:text-ink"
          >
            <X size={19} aria-hidden="true" />
          </button>
        </div>

        {children}

        {footer && <div className="mt-5 flex flex-wrap justify-end gap-2 border-t border-line pt-4">{footer}</div>}
      </div>
    </div>
  );
}

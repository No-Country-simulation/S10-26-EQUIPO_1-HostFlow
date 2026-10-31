import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@utils/format";

// Carrusel de fotos del alojamiento.
//
// Se usa en la portada del onboarding para presentar el lugar antes de empezar
// la ficha. Reglas de UX que respeta:
// - Avance automático cada 2 s, que se pausa al pasar el mouse o enfocar.
// - Navegar a mano no lo apaga: reinicia la cuenta y sigue solo.
// - Respeta `prefers-reduced-motion` y suma swipe en móvil.
// - Con una sola foto no muestra controles; con cero no renderiza nada.
const AUTOPLAY_MS = 2000;
const SWIPE_PX = 40;

// Una diapositiva del carrusel. El encuadre depende de la forma de la foto:
// - Horizontal o cuadrada: llena el marco (`object-cover`), que en escritorio
//   tiene proporción casi 3:2, así las horizontales se lucen sin recortes raros.
// - Vertical: entra completa (`object-contain`) y el espacio libre se rellena
//   con la misma foto desenfocada, para no recortarla.
const PORTRAIT_RATIO = 1.05;

function Slide({ src, name, index, active }) {
  const [contain, setContain] = useState(false);

  return (
    <div
      className={cn(
        "absolute inset-0 transition-opacity duration-500 motion-reduce:transition-none",
        active ? "opacity-100" : "opacity-0",
      )}
    >
      {contain && (
        <img
          src={src}
          alt=""
          aria-hidden="true"
          draggable="false"
          className="pointer-events-none absolute inset-0 size-full scale-110 select-none object-cover brightness-[0.55] blur-2xl"
        />
      )}
      <img
        src={src}
        alt={`${name} · foto ${index + 1}`}
        aria-hidden={!active}
        loading={index === 0 ? "eager" : "lazy"}
        draggable="false"
        onLoad={(event) => {
          const { naturalWidth, naturalHeight } = event.currentTarget;
          setContain(naturalHeight > naturalWidth * PORTRAIT_RATIO);
        }}
        className={cn(
          "relative size-full select-none object-center",
          contain ? "object-contain" : "object-cover",
        )}
      />
    </div>
  );
}

export default function PhotoCarousel({ images = [], name = "Alojamiento", className }) {
  const photos = images.filter(Boolean);
  const count = photos.length;
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const touchX = useRef(null);

  // Si cambia la cantidad de fotos, el índice vuelve a un valor válido.
  useEffect(() => {
    setIndex((current) => (current < count ? current : 0));
  }, [count]);

  // Un timeout por diapositiva: cualquier cambio de índice (automático o
  // manual) reinicia la cuenta, así el carrusel sigue avanzando solo.
  useEffect(() => {
    if (count < 2 || paused) return undefined;
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches) return undefined;
    const timer = window.setTimeout(() => setIndex((current) => (current + 1) % count), AUTOPLAY_MS);
    return () => window.clearTimeout(timer);
  }, [count, index, paused]);

  if (count === 0) return null;

  const go = (target) => {
    setIndex(((target % count) + count) % count);
  };

  const arrow =
    "absolute top-1/2 z-10 grid size-9 -translate-y-1/2 cursor-pointer place-items-center rounded-full bg-black/40 text-white backdrop-blur-sm transition hover:bg-black/60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white";

  return (
    <section
      aria-roledescription="carrusel"
      aria-label={`Fotos de ${name}`}
      className={cn("relative isolate overflow-hidden bg-cloud", className)}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
      onKeyDown={(event) => {
        if (count < 2) return;
        if (event.key === "ArrowLeft") {
          event.preventDefault();
          go(index - 1);
        }
        if (event.key === "ArrowRight") {
          event.preventDefault();
          go(index + 1);
        }
      }}
      onTouchStart={(event) => {
        touchX.current = event.touches[0]?.clientX ?? null;
      }}
      onTouchEnd={(event) => {
        if (touchX.current === null) return;
        const end = event.changedTouches[0]?.clientX ?? touchX.current;
        const delta = end - touchX.current;
        touchX.current = null;
        if (Math.abs(delta) > SWIPE_PX) go(index + (delta < 0 ? 1 : -1));
      }}
    >
      {/* Móvil: marco 3:2 compacto. Tablet (una columna ancha): 16:9 sin tope de
          altura, así la foto horizontal casi no se recorta. Escritorio: el marco
          llena la columna al lado del saludo, con proporción cercana a 3:2. */}
      <div className="relative aspect-[3/2] max-h-[280px] w-full sm:aspect-[16/9] sm:max-h-none lg:aspect-auto lg:h-full lg:min-h-[320px]">
        {photos.map((src, i) => (
          <Slide key={i} src={src} name={name} index={i} active={i === index} />
        ))}
      </div>

      {count > 1 && (
        <>
          <button type="button" aria-label="Foto anterior" onClick={() => go(index - 1)} className={cn(arrow, "left-3")}>
            <ChevronLeft size={18} aria-hidden="true" />
          </button>
          <button type="button" aria-label="Foto siguiente" onClick={() => go(index + 1)} className={cn(arrow, "right-3")}>
            <ChevronRight size={18} aria-hidden="true" />
          </button>

          <span className="absolute right-3 top-3 rounded-full bg-black/45 px-2.5 py-1 text-[11px] font-bold text-white backdrop-blur-sm">
            {index + 1} / {count}
          </span>

          <div className="absolute inset-x-0 bottom-3 flex justify-center">
            <div className="flex items-center gap-1.5 rounded-full bg-black/30 px-2.5 py-1.5 backdrop-blur-sm">
              {photos.map((_, i) => (
                <button
                  key={i}
                  type="button"
                  aria-label={`Ir a la foto ${i + 1}`}
                  aria-current={i === index}
                  onClick={() => go(i)}
                  className={cn(
                    "h-1.5 cursor-pointer rounded-full transition-all focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white",
                    i === index ? "w-5 bg-white" : "w-1.5 bg-white/55 hover:bg-white/85",
                  )}
                />
              ))}
            </div>
          </div>

          <p className="sr-only" aria-live="polite">
            Foto {index + 1} de {count}
          </p>
        </>
      )}
    </section>
  );
}

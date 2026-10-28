import { ArrowRight, ClipboardList, Clock, HeartPulse, Ship, ShieldCheck, UserRound } from "lucide-react";
import { Badge, BrandLogo, Button, WhatsAppIcon } from "@components/ui";
import PhotoCarousel from "./PhotoCarousel";
import StayCard from "./StayCard";

// Portada del onboarding: presenta el alojamiento con sus fotos antes de pedir
// datos. Es una pantalla de bienvenida, no un paso del formulario: no cuenta en
// el progreso y se puede retomar desde el paso 1 con "Atrás".
// En escritorio la foto va al costado del saludo (marco casi 3:2, así las fotos
// horizontales se lucen completas) y el plan + CTA quedan en una banda abajo.
const PLAN = [
  { icon: UserRound, title: "Tus datos", text: "Nombre, documento y contacto." },
  { icon: HeartPulse, title: "Alergias y preferencias", text: "Para recibirte como necesitás." },
  { icon: ClipboardList, title: "Servicios", text: "Desayuno, excursiones y extras." },
  { icon: Ship, title: "Traslado", text: "Lancha hasta el alojamiento (opcional)." },
];

export default function OnboardingIntro({ lodging, booking, nights, photos = [], stepsCount = 5, onStart, waLink }) {
  const firstName = String(booking?.guestName || "").trim().split(/\s+/)[0];
  const waDigits = String(lodging.phone || "").replace(/\D/g, "");

  return (
    <div className="card animate-rise overflow-hidden">
      <div className="lg:grid lg:grid-cols-[1.05fr_0.95fr]">
        {/* Presentación: carrusel de fotos; sin fotos, una portada con la marca. */}
        {photos.length > 0 ? (
          <PhotoCarousel images={photos} name={lodging.name} className="lg:h-full" />
        ) : (
          <div
            className="relative grid h-44 place-items-center sm:h-52 lg:h-full"
            style={{ backgroundColor: "var(--hf-hero)", color: "var(--hf-hero-ink)" }}
          >
            <div className="flex flex-col items-center gap-2.5 px-6 text-center">
              <BrandLogo src={lodging.logo} name={lodging.name} size="lg" />
              <p className="font-display text-xl font-extrabold leading-tight">{lodging.name}</p>
              <p className="text-[11px] font-semibold opacity-80">{lodging.location}</p>
            </div>
          </div>
        )}

        {/* Saludo + resumen de la estadía */}
        <div className="grid content-center gap-4 p-6 sm:p-8">
          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-teal-strong">
              Tu llegada a {lodging.name}
            </p>
            <h1 className="mt-1 font-display text-[28px] font-extrabold leading-tight text-ink sm:text-3xl">
              {firstName ? `¡Hola, ${firstName}!` : "¡Te damos la bienvenida!"}
            </h1>
            <p className="mt-2 text-sm text-muted">
              Completá tu ficha de llegada en {stepsCount} pasos cortos. Así {lodging.name} sabe cuándo llegás y qué
              necesitás.
            </p>
          </div>

          <StayCard booking={booking} lodging={lodging} nights={nights} />
        </div>
      </div>

      {/* Plan + acción principal */}
      <div className="border-t border-line p-6 sm:p-8">
        <div className="grid gap-5 lg:grid-cols-[1fr_auto] lg:items-center lg:gap-8">
          <div className="min-w-0">
            <p className="text-[11px] font-extrabold uppercase tracking-[0.14em] text-muted">Lo que vas a completar</p>
            <ul className="mt-2.5 grid gap-2 sm:grid-cols-2">
              {PLAN.map(({ icon: Icon, title, text }) => (
                <li key={title} className="flex items-start gap-2.5 rounded-xl border border-line bg-cloud p-3">
                  <span className="grid size-7 shrink-0 place-items-center rounded-[10px] bg-teal-soft text-teal-strong">
                    <Icon size={14} aria-hidden="true" />
                  </span>
                  <span className="min-w-0">
                    <span className="block text-xs font-bold text-ink">{title}</span>
                    <span className="block text-[11px] leading-snug text-muted">{text}</span>
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div className="grid gap-2.5 lg:w-64">
            <Button type="button" onClick={onStart} className="w-full justify-center">
              Empezar
              <ArrowRight size={18} aria-hidden="true" />
            </Button>

            <div className="flex flex-wrap items-center gap-1.5">
              <Badge tone="teal">
                <Clock size={11} /> 1 minuto
              </Badge>
              <Badge tone="lime">
                <ShieldCheck size={11} /> Sin cuenta
              </Badge>
            </div>

            <p className="text-[11px] leading-snug text-muted">
              Podés volver atrás en cualquier momento. No necesitás crear una cuenta.
            </p>

            {waDigits > 0 && (
              <a
                href={waLink(`Hola ${lodging.name}, tengo una consulta sobre mi llegada.`)}
                target="_blank"
                rel="noreferrer"
                className="inline-flex w-fit items-center gap-2 rounded-full border border-teal/40 bg-teal-soft px-3.5 py-2 text-xs font-bold text-teal-strong transition-colors hover:border-teal hover:bg-teal/15"
              >
                <WhatsAppIcon size={15} /> Escribinos por WhatsApp
              </a>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

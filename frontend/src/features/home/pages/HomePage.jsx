import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import {
  ArrowRight,
  ArrowUpRight,
  BedDouble,
  CalendarCheck,
  Check,
  ChevronDown,
  Coffee,
  CreditCard,
  MapPin,
  Menu,
  Palette,
  Quote,
  ShieldCheck,
  Sparkles,
  Star,
  X,
} from "lucide-react";
import { Brand, buttonClass, DemoBanner, ThemeToggle } from "@components/ui";
import { cn } from "@utils/format";

const features = [
  {
    icon: CalendarCheck,
    title: "Reservas en tiempo real",
    text: "Los huéspedes reservan las 24 horas, los 7 días. Vos confirmás con un toque.",
    tone: "lime",
  },
  {
    icon: BedDouble,
    title: "Tu alojamiento, a tu manera",
    text: "Habitaciones, baños, precio e imágenes. Y una marca que se adapta a tu estilo.",
    tone: "teal",
  },
  {
    icon: CreditCard,
    title: "Cobrá como quieras",
    text: "Pesos argentinos como moneda principal, dólares y euros como alternativa, y billeteras digitales.",
    tone: "pink",
  },
  {
    icon: ShieldCheck,
    title: "Acceso seguro",
    text: "Ingreso con OTP de 6 dígitos y recuperación de clave en pasos simples.",
    tone: "teal",
  },
];

const steps = [
  { title: "Creá tu alojamiento", text: "Registra tu propiedad en minutos: nombre, capacidad y precio." },
  { title: "Personalizá tu marca", text: "Subí tu logo y elegí los colores que representan a tu empresa." },
  { title: "Compartí y gestioná", text: "Recibí huéspedes, confirmá reservas y bloqueá los días que necesites." },
];

const faqs = [
  { q: "¿Los huéspedes necesitan crear una cuenta?", a: "No. Acceden desde un enlace único que vos les compartís, sin registros ni contraseñas." },
  { q: "¿Cómo funciona la recuperación de clave?", a: "Ingresás tu correo, recibís un código OTP de 6 dígitos y definís tu nueva clave con doble confirmación." },
  { q: "¿Qué monedas puedo cobrar?", a: "Pesos argentinos como moneda principal, con dólares y euros como secundarias, más billeteras digitales." },
  { q: "¿Puedo pausar mi alojamiento?", a: "Sí. Desde Disponibilidad bloqueás los días que no querés recibir huéspedes." },
];

const plans = [
  {
    name: "Básico",
    price: "Gratis",
    tagline: "Para empezar a recibir huéspedes",
    features: ["1 alojamiento", "Reservas ilimitadas", "Cobro en pesos argentinos", "Soporte por correo"],
    cta: "Comenzar gratis",
    highlight: false,
  },
  {
    name: "Pro",
    price: "$29.99",
    period: "/mes",
    tagline: "Para alojamientos en crecimiento",
    features: [
      "Todo lo del plan Básico",
      "Monedas secundarias (USD · EUR)",
      "Billeteras digitales",
      "Bloqueo de días y calendario",
      "Soporte prioritario 24/7",
    ],
    cta: "Probar 14 días gratis",
    highlight: true,
  },
];

export default function HomePage() {
  return (
    <div className="min-h-dvh bg-paper text-ink antialiased">
      <DemoBanner />
      <Header />
      <Hero />
      <LogoMarquee />
      <Gallery />
      <Features />
      <HowItWorks />
      <Testimonial />
      <Stats />
      <Pricing />
      <FAQ />
      <FinalCTA />
      <Footer />
    </div>
  );
}

// ── Reveal (scroll) ─────────────────────────────────────────────────────────
// Aparece con un fundido + desplazamiento cuando la sección entra en pantalla.
// Si no hay IntersectionObserver (o el usuario prefiere menos movimiento) se
// muestra directamente: nunca deja contenido invisible.
function Reveal({ as: Element = "div", delay = 0, className, children, ...props }) {
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

// ── Header ──────────────────────────────────────────────────────────────────

function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={cn(
        "sticky top-0 z-50 transition-all duration-300",
        scrolled ? "glass border-b border-line" : "bg-transparent",
      )}
    >
      <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-4 sm:px-6">
        <Brand />

        <nav className="hidden items-center gap-1 md:flex">
          {[
            ["#caracteristicas", "Características"],
            ["#como-funciona", "Cómo funciona"],
            ["#precios", "Precios"],
          ].map(([href, label]) => (
            <a
              key={href}
              href={href}
              className="relative rounded-lg px-4 py-2 text-sm font-semibold text-ink-soft transition-colors hover:text-ink"
            >
              {label}
            </a>
          ))}
        </nav>

        <div className="hidden items-center gap-2.5 md:flex">
          <ThemeToggle />
          <Link to="/auth/login" className="px-4 py-2 text-sm font-semibold text-ink-soft transition-colors hover:text-ink">
            Ingresar
          </Link>
          <Link to="/auth/registro" className={buttonClass("primary", "min-h-[44px]")}>
            Crear cuenta
          </Link>
        </div>

        <div className="flex items-center gap-2 md:hidden">
          <ThemeToggle />
          <button onClick={() => setOpen((v) => !v)} aria-label="Menú" className="grid size-10 cursor-pointer place-items-center rounded-xl border border-line bg-paper text-ink">
            {open ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>

      {open && (
        <div className="glass border-t border-line px-5 py-4 md:hidden animate-fade">
          <nav className="grid gap-1">
            {[
              ["#caracteristicas", "Características"],
              ["#como-funciona", "Cómo funciona"],
              ["#precios", "Precios"],
            ].map(([href, label]) => (
              <a key={href} href={href} onClick={() => setOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-semibold text-ink-soft hover:bg-overlay">
                {label}
              </a>
            ))}
          </nav>
          <div className="mt-3 grid grid-cols-2 gap-2">
            <Link to="/auth/login" onClick={() => setOpen(false)} className={buttonClass("secondary")}>
              Ingresar
            </Link>
            <Link to="/auth/registro" onClick={() => setOpen(false)} className={buttonClass("primary")}>
              Crear cuenta
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}

// ── Hero ────────────────────────────────────────────────────────────────────

function Hero() {
  return (
    <section className="relative overflow-hidden px-5 pt-14 pb-16 sm:px-6 lg:pt-20 lg:pb-24">
      <div className="pointer-events-none absolute inset-0 -z-10" aria-hidden="true">
        <div className="absolute -top-28 right-[-8%] size-[520px] rounded-full bg-lime/25 blur-[140px]" />
        <div className="absolute bottom-[-32%] left-[-12%] size-[440px] rounded-full bg-teal/15 blur-[140px]" />
        <div className="absolute inset-0 dot-grid opacity-35 [mask-image:radial-gradient(ellipse_at_center,black,transparent_72%)]" />
      </div>

      <div className="mx-auto max-w-6xl">
        <div className="grid items-center gap-12 lg:grid-cols-[1.02fr_0.98fr] lg:gap-8">
          <div className="animate-rise">
            <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-line bg-paper px-4 py-1.5 text-xs font-bold text-ink-soft shadow-card">
              <span className="relative flex size-2">
                <span className="absolute inline-flex size-full animate-ping rounded-full bg-teal opacity-60" />
                <span className="relative inline-flex size-2 rounded-full bg-teal" />
              </span>
              Nuevo · Gestión de alojamientos del Delta
            </span>

            <h1 className="text-[clamp(38px,6.4vw,68px)] font-extrabold leading-[1.02] tracking-[-0.035em]">
              Recibí a tus huéspedes con{" "}
              <span className="text-gradient-lime underline-hand">fluidez</span>
            </h1>

            <p className="mt-6 max-w-lg text-lg leading-relaxed text-muted">
              HostFlow centraliza reservas, adicionales y disponibilidad en una plataforma
              con carácter, pensada para alojamientos que quieren profesionalizar su gestión
              sin perder la calidez de siempre.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-4">
              <Link to="/auth/registro" className={buttonClass("primary", "min-h-[54px] px-7 text-[15px]")}>
                Comenzar gratis <ArrowRight size={18} />
              </Link>
              <a href="#como-funciona" className="group inline-flex items-center gap-2 text-sm font-bold text-ink-soft transition-colors hover:text-ink">
                Ver cómo funciona
                <ArrowRight size={15} className="transition-transform group-hover:translate-x-1" />
              </a>
            </div>

            <div className="mt-10 flex items-center gap-4">
              <div className="flex -space-x-2.5">
                {["M", "L", "C", "J"].map((initial, index) => (
                  <span
                    key={initial}
                    className={cn(
                      "grid size-10 place-items-center rounded-full border-2 border-paper text-xs font-extrabold shadow-sm",
                      index % 2 ? "bg-teal-soft text-teal-strong" : "bg-lime text-lime-ink",
                    )}
                  >
                    {initial}
                  </span>
                ))}
              </div>
              <div>
                <div className="flex items-center gap-1">
                  {[...Array(5)].map((_, i) => (
                    <Star key={i} size={15} className="fill-ink text-ink" />
                  ))}
                  <span className="ml-1 text-sm font-extrabold">4.9</span>
                </div>
                <p className="text-xs text-muted">+50 anfitriones confían en HostFlow</p>
              </div>
            </div>
          </div>

          {/* Escena ilustrada del Delta: dibujo propio, no stock ni render. */}
          <Reveal className="relative animate-rise" delay={120}>
            <div className="grain relative overflow-hidden rounded-[30px] border border-line shadow-pop">
              <DeltaScene />

              {/* Stickers sobre la escena */}
              <span
                className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-paper/90 px-3 py-1.5 text-[11px] font-extrabold text-ink shadow-card backdrop-blur animate-drift"
                style={{ "--drift-rotate": "-2deg" }}
              >
                <MapPin size={13} className="text-teal-strong" /> Delta del Tigre
              </span>

              <div
                className="absolute bottom-4 left-4 right-4 flex items-center gap-3 rounded-2xl border border-line bg-paper/92 p-3 shadow-pop backdrop-blur-sm sm:left-auto sm:w-[264px]"
                style={{ animationDelay: "600ms" }}
              >
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-lime text-lime-ink animate-flicker">
                  <CalendarCheck size={18} />
                </span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-extrabold text-ink">María G. · 3 huéspedes</p>
                  <p className="truncate text-xs text-muted">Check-in mañana · 14:00 hs</p>
                </div>
                <span className="ml-auto shrink-0 rounded-full bg-lime-soft px-2 py-1 text-[10px] font-extrabold text-lime-ink">
                  Confirmado
                </span>
              </div>
            </div>

            <span
              className="absolute -right-3 -top-4 hidden rotate-6 rounded-full bg-pink px-3.5 py-1.5 text-[11px] font-extrabold text-ink shadow-card sm:block animate-drift"
              style={{ "--drift-rotate": "6deg", animationDelay: "900ms" }}
            >
              sin comisiones
            </span>
          </Reveal>
        </div>
      </div>
    </section>
  );
}

// Escena del río dibujada a mano en SVG: cielo, sol, arboleda, cabaña, muelle,
// lancha y agua en movimiento. Los colores salen de los tokens del tema, así
// funciona en modo claro y oscuro (y con la marca de un alojamiento).
function DeltaScene() {
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
          <path d="M628 560c-2-32-14-50-28-64" opacity="0.75" />
        </g>
      </g>
    </svg>
  );
}

function Tree({ x, y, s = 1, o = 0.5 }) {
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`} opacity={o}>
      <rect x="-5" y="0" width="10" height="86" rx="4" fill="var(--color-ink)" opacity="0.75" />
      <ellipse cx="0" cy="-18" rx="46" ry="40" fill="var(--color-teal)" />
      <ellipse cx="-30" cy="6" rx="28" ry="24" fill="var(--color-teal)" opacity="0.9" />
      <ellipse cx="30" cy="4" rx="26" ry="22" fill="var(--color-teal)" opacity="0.9" />
    </g>
  );
}

// ── Marquee ─────────────────────────────────────────────────────────────────

// Iniciales de un alojamiento, salteando artículos y preposiciones.
const TRUST_STOPWORDS = new Set(["del", "de", "la", "las", "los", "el", "y"]);
const trustInitials = (name) =>
  name
    .split(" ")
    .filter((word) => word.length >= 2 && !TRUST_STOPWORDS.has(word.toLowerCase()))
    .slice(0, 2)
    .map((word) => word[0])
    .join("")
    .toUpperCase();

// Tonos de marca sólidos (los mismos del hero): se leen bien en claro y oscuro,
// a diferencia de los tintes suaves que sobre el fondo oscuro quedaban turbios.
const TRUST_TONES = ["bg-lime text-lime-ink", "bg-teal text-white", "bg-pink text-ink"];

function LogoMarquee() {
  const names = ["Cabañas del Río", "Posada Delta", "Albergue La Lancha", "Hotel Tigre", "Lo del Mono", "Mirador del Paraná", "Casa Flotante"];
  // Dos grupos IDÉNTICOS: el keyframe mueve el track un -50 %, que equivale
  // exactamente al ancho de un grupo. Así el bucle cierra sin saltos (antes el
  // `gap` y el `pr` de la lista duplicada no coincidían y la animación “cortaba”
  // al reiniciar).
  const groups = [0, 1];

  return (
    <section className="relative overflow-hidden border-y border-line bg-cloud py-8 sm:py-9">
      {/* Trama diagonal fina: da textura sin competir con las etiquetas. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 opacity-[0.1]"
        style={{
          backgroundImage:
            "repeating-linear-gradient(-45deg, var(--color-ink) 0 1px, transparent 1px 9px)",
        }}
      />
      {/* Manchas de marca apenas visibles: solo un halo de color en los bordes,
          sin el degradado fuerte que dominaba la sección. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -left-28 top-1/2 size-40 -translate-y-1/2 rounded-full bg-lime/10 blur-3xl"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-28 top-1/2 size-40 -translate-y-1/2 rounded-full bg-teal/10 blur-3xl"
      />

      <div className="relative mx-auto max-w-6xl px-5 sm:px-6">
        {/* Rótulo centrado con punto de marca: el recurso de eyebrow del home,
            sin las reglas largas a los costados. */}
        <p className="mb-5 flex items-center justify-center gap-2 text-[11px] font-extrabold uppercase tracking-[0.18em] text-muted">
          <span aria-hidden="true" className="size-1.5 rounded-full bg-lime" />
          Confían en HostFlow
        </p>

        <div className="group relative overflow-hidden [mask-image:linear-gradient(90deg,transparent,black_8%,black_92%,transparent)]">
          <div className="flex w-max animate-marquee group-hover:[animation-play-state:paused] motion-reduce:animate-none">
            {groups.map((group) => (
              <ul
                key={group}
                aria-hidden={group === 1 ? "true" : undefined}
                className="flex shrink-0 items-center gap-3 pr-3"
              >
                {names.map((name, index) => (
                  <li
                    key={`${group}-${name}`}
                    className="flex shrink-0 items-center gap-2 rounded-full border border-line-strong bg-paper py-1.5 pl-1.5 pr-4"
                  >
                    <span
                      aria-hidden="true"
                      className={cn(
                        "grid size-6 place-items-center rounded-full text-[10px] font-extrabold",
                        TRUST_TONES[index % TRUST_TONES.length],
                      )}
                    >
                      {trustInitials(name)}
                    </span>
                    <span className="whitespace-nowrap text-[13px] font-bold text-ink">{name}</span>
                  </li>
                ))}
              </ul>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

// ── Gallery: "Así se vive" ──────────────────────────────────────────────────
// Tres láminas ilustradas en formato polaroid, con cinta y rotación: un bloque
// editorial que rompe la grilla perfecta típica de las landing generadas.

function Gallery() {
  const cards = [
    {
      art: <SceneLancha />,
      title: "La llegada en lancha",
      text: "El huésped desembarca en tu muelle con su ficha ya cargada.",
      tilt: "-rotate-2",
      delay: 0,
    },
    {
      art: <SceneInterior />,
      title: "La casa lista",
      text: "Fechas bloqueadas, reservas confirmadas y todo al día.",
      tilt: "rotate-1",
      delay: 110,
    },
    {
      art: <SceneDesayuno />,
      title: "Los adicionales",
      text: "Desayunos y excursiones que el huésped suma desde su enlace.",
      tilt: "rotate-3",
      delay: 220,
    },
  ];

  return (
    <section className="mx-auto max-w-6xl px-5 py-20 sm:px-6 lg:py-24">
      <Reveal className="max-w-2xl">
        <span className="mb-3 inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-['']">
          Así se vive
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
          Del desembarco al <span className="font-display italic">desayuno</span>, todo en un lugar
        </h2>
        <p className="mt-4 text-lg leading-relaxed text-muted">
          Tres momentos chicos del día a día de un alojamiento, resueltos en la misma plataforma.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-8 sm:grid-cols-2 lg:grid-cols-3 lg:gap-6">
        {cards.map((card, index) => (
          <Reveal key={card.title} delay={card.delay}>
            <figure
              className={cn(
                "tape group relative rotate-0 rounded-[22px] border border-line bg-paper p-3 pb-5 shadow-card transition-all duration-300",
                card.tilt,
                "hover:rotate-0 hover:-translate-y-1.5 hover:shadow-pop",
              )}
              style={{ animationDelay: `${index * 60}ms` }}
            >
              <div className="grain relative overflow-hidden rounded-[16px] border border-line">
                {card.art}
              </div>
              <figcaption className="px-1.5 pt-4">
                <p className="font-display text-lg font-semibold text-ink">{card.title}</p>
                <p className="mt-1 text-sm leading-relaxed text-muted">{card.text}</p>
              </figcaption>
            </figure>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

function SceneFrame({ children, label }) {
  return (
    <svg viewBox="0 0 400 300" className="block h-auto w-full" role="img" aria-label={label}>
      {children}
    </svg>
  );
}

function SceneLancha() {
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

function SceneInterior() {
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

function SceneDesayuno() {
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

// ── Features ────────────────────────────────────────────────────────────────

function Features() {
  const tones = { lime: "bg-lime text-lime-ink", teal: "bg-teal-soft text-teal-strong", pink: "bg-pink-soft text-pink-strong" };
  return (
    <section id="caracteristicas" className="mx-auto max-w-6xl px-5 py-20 sm:px-6 lg:py-28">
      <Reveal className="max-w-2xl">
        <span className="mb-3 inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-['']">
          Características
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
          Todo lo que necesitás, <span className="text-gradient-lime">nada que sobre</span>
        </h2>
        <p className="mt-4 text-lg leading-relaxed text-muted">
          Herramientas pensadas para pequeños y medianos alojamientos que quieren crecer sin complicarse.
        </p>
      </Reveal>

      <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {features.map((feature, index) => (
          <Reveal key={feature.title} delay={index * 90}>
            <article className="card card-hover group relative h-full overflow-hidden p-6">
              <span
                aria-hidden="true"
                className="absolute -right-4 -top-5 font-display text-[86px] font-semibold leading-none text-ink/[0.05] transition-transform duration-500 group-hover:-translate-y-1 group-hover:rotate-6"
              >
                {index + 1}
              </span>
              <span className={cn("relative mb-5 grid size-12 place-items-center rounded-2xl transition-transform duration-300 group-hover:-translate-y-1 group-hover:scale-110", tones[feature.tone])}>
                <feature.icon size={22} />
              </span>
              <h3 className="relative mb-2 font-bold text-ink">{feature.title}</h3>
              <p className="relative text-sm leading-relaxed text-muted">{feature.text}</p>
            </article>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

// ── How it works ────────────────────────────────────────────────────────────

function HowItWorks() {
  return (
    <section id="como-funciona" className="relative overflow-hidden border-y border-line bg-cloud py-20 lg:py-28">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 opacity-[0.07]"
        style={{
          backgroundImage: "radial-gradient(var(--color-ink) 1.2px, transparent 1.2px)",
          backgroundSize: "26px 26px",
        }}
      />
      <div className="relative mx-auto max-w-6xl px-5 sm:px-6">
        <Reveal className="mx-auto max-w-2xl text-center">
          <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
            Cómo funciona
          </span>
          <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">Simple desde el primer día</h2>
        </Reveal>

        <div className="mt-16 grid gap-10 md:grid-cols-3 md:gap-8">
          {steps.map((step, index) => (
            <Reveal key={step.title} delay={index * 120} className="relative text-center">
              {index < steps.length - 1 && (
                <span
                  aria-hidden="true"
                  className="absolute left-[calc(50%+46px)] top-10 hidden h-px w-[calc(100%-92px)] border-t-2 border-dashed border-line-strong md:block"
                />
              )}
              <span className="relative mx-auto mb-6 grid size-20 place-items-center rounded-3xl border border-line bg-paper text-2xl font-extrabold text-ink shadow-card">
                <span className="font-display italic">{index + 1}</span>
                <span className="absolute -right-1.5 -top-1.5 grid size-6 place-items-center rounded-full bg-lime text-[11px] font-extrabold text-lime-ink">
                  ✓
                </span>
              </span>
              <h3 className="mb-2 text-lg font-bold text-ink">{step.title}</h3>
              <p className="mx-auto max-w-xs text-sm leading-relaxed text-muted">{step.text}</p>
            </Reveal>
          ))}
        </div>

        <Reveal className="mt-14 flex justify-center" delay={120}>
          <Link to="/auth/registro" className={buttonClass("primary", "min-h-[52px] px-7")}>
            Empezar ahora <ArrowRight size={18} />
          </Link>
        </Reveal>
      </div>
    </section>
  );
}

// ── Testimonio ──────────────────────────────────────────────────────────────

function Testimonial() {
  return (
    <section className="mx-auto max-w-6xl px-5 py-20 sm:px-6 lg:py-24">
      <Reveal className="grid items-center gap-10 lg:grid-cols-[1.4fr_1fr]">
        <figure className="relative rounded-[28px] border border-line bg-cloud p-7 sm:p-10">
          <Quote size={40} className="mb-4 text-lime-strong" aria-hidden="true" />
          <blockquote className="font-display text-[clamp(20px,2.6vw,30px)] font-medium italic leading-[1.35] text-ink">
            “Antes manejaba todo por WhatsApp y se me cruzaban los días. Con HostFlow las reservas
            están en un calendario, los huéspedes completan su ficha antes de llegar y yo duermo
            tranquila.”
          </blockquote>
          <figcaption className="mt-7 flex items-center gap-3.5">
            <span className="grid size-12 shrink-0 place-items-center rounded-full bg-teal text-sm font-extrabold text-white shadow-card">
              CM
            </span>
            <div>
              <p className="font-bold text-ink">Carolina Méndez</p>
              <p className="text-sm text-muted">Propietaria · Posada Delta, Tigre</p>
            </div>
            <span className="ml-auto hidden gap-1 sm:flex" aria-label="5 de 5 estrellas">
              {[...Array(5)].map((_, i) => (
                <Star key={i} size={15} className="fill-ink text-ink" />
              ))}
            </span>
          </figcaption>
        </figure>

        <div className="relative mx-auto w-full max-w-[300px] rotate-2 transition-transform duration-300 hover:rotate-0">
          <div className="tape grain relative overflow-hidden rounded-[20px] border border-line bg-paper p-3 pb-14 shadow-pop">
            <div className="grid aspect-[4/5] place-items-center rounded-[14px] bg-gradient-to-br from-teal-soft via-lime-soft to-pink-soft">
              <div className="grid place-items-center gap-2 text-center">
                <span className="grid size-20 place-items-center rounded-full bg-paper font-display text-3xl font-semibold text-ink shadow-card">
                  <Coffee size={30} aria-hidden="true" />
                </span>
                <p className="max-w-[180px] px-3 text-xs font-semibold text-ink-soft">
                  6:40 · ya está el café para la llegada de las 7
                </p>
              </div>
            </div>
            <p className="absolute inset-x-4 bottom-4 text-center font-display text-sm italic text-muted">
              — del cuaderno de una anfitriona
            </p>
          </div>
        </div>
      </Reveal>
    </section>
  );
}

// ── Stats ───────────────────────────────────────────────────────────────────

function Stats() {
  const stats = [
    { value: "50+", label: "Alojamientos activos" },
    { value: "1.2k", label: "Huéspedes gestionados" },
    { value: "98%", label: "Satisfacción" },
    { value: "24/7", label: "Soporte" },
  ];
  return (
    <section className="mx-auto max-w-6xl px-5 py-16 sm:px-6">
      <Reveal className="relative overflow-hidden rounded-[28px] border border-line bg-gradient-to-r from-lime-soft via-teal-soft to-pink-soft px-5 py-10 sm:px-8 sm:py-12">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-30 [mask-image:radial-gradient(ellipse_at_left,black,transparent_70%)]"
          style={{
            backgroundImage:
              "repeating-linear-gradient(-45deg, var(--color-ink) 0 1px, transparent 1px 10px)",
          }}
        />
        <div className="relative grid grid-cols-2 gap-6 md:grid-cols-4 sm:gap-8">
          {stats.map((stat, index) => (
            <div key={stat.label} className="text-center" style={{ animationDelay: `${index * 70}ms` }}>
              <p className="text-[clamp(30px,4vw,44px)] font-extrabold leading-none text-ink">{stat.value}</p>
              <p className="mt-2 text-sm font-semibold text-ink-soft">{stat.label}</p>
            </div>
          ))}
        </div>
      </Reveal>
    </section>
  );
}

// ── Pricing ─────────────────────────────────────────────────────────────────

function Pricing() {
  return (
    <section id="precios" className="border-y border-line bg-cloud py-20 lg:py-28">
      <div className="mx-auto max-w-5xl px-5 sm:px-6">
        <Reveal className="mx-auto max-w-2xl text-center">
          <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
            Precios
          </span>
          <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
            Todas las características incluidas
          </h2>
          <p className="mt-4 text-lg text-muted">Sin compromiso. Cancelá cuando quieras.</p>
        </Reveal>

        <div className="mt-14 grid gap-6 md:grid-cols-2">
          {plans.map((plan, index) => (
            <Reveal key={plan.name} delay={index * 110}>
              <article
                className={cn(
                  "card relative h-full overflow-hidden p-6 sm:p-8",
                  plan.highlight && "border-lime ring-2 ring-lime/40",
                )}
              >
                {plan.highlight && (
                  <span className="absolute right-5 top-5 rounded-full bg-lime px-3 py-1 text-[11px] font-extrabold text-lime-ink">
                    Recomendado
                  </span>
                )}
                <h3 className="font-extrabold text-ink">{plan.name}</h3>
                <p className="mt-1 text-sm text-muted">{plan.tagline}</p>
                <div className="mt-5 flex items-end gap-1">
                  <span className="text-[clamp(36px,5vw,52px)] font-extrabold leading-none tracking-tight text-ink">{plan.price}</span>
                  {plan.period && <span className="pb-1 text-sm font-semibold text-muted">{plan.period}</span>}
                </div>
                <ul className="mt-7 grid gap-3">
                  {plan.features.map((feature) => (
                    <li key={feature} className="flex items-start gap-2.5 text-sm text-ink-soft">
                      <Check size={16} className="mt-0.5 shrink-0 text-teal-strong" />
                      {feature}
                    </li>
                  ))}
                </ul>
                <Link
                  to="/auth/registro"
                  className={buttonClass(plan.highlight ? "primary" : "secondary", "mt-8 w-full")}
                >
                  {plan.cta} <ArrowRight size={17} />
                </Link>
              </article>
            </Reveal>
          ))}
        </div>

        <p className="mt-8 text-center text-xs text-muted">
          +$20 por usuario adicional al mes, más impuestos. *Procesamiento de pagos no incluido.
        </p>
      </div>
    </section>
  );
}

// ── FAQ ─────────────────────────────────────────────────────────────────────

function FAQ() {
  const [open, setOpen] = useState(0);
  return (
    <section className="mx-auto max-w-3xl px-5 py-20 sm:px-6 lg:py-28">
      <Reveal className="mb-12 text-center">
        <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
          FAQ
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">Preguntas frecuentes</h2>
      </Reveal>

      <div className="grid gap-3">
        {faqs.map((faq, index) => {
          const isOpen = open === index;
          return (
            <Reveal key={faq.q} delay={index * 70}>
              <div className="card overflow-hidden">
                <button
                  id={`faq-btn-${index}`}
                  onClick={() => setOpen(isOpen ? -1 : index)}
                  aria-expanded={isOpen}
                  aria-controls={`faq-panel-${index}`}
                  className="flex w-full cursor-pointer items-center justify-between gap-4 px-6 py-4 text-left"
                >
                  <span className="font-bold text-ink">{faq.q}</span>
                  <ChevronDown size={18} aria-hidden="true" className={cn("shrink-0 text-muted transition-transform duration-300", isOpen && "rotate-180")} />
                </button>
                {isOpen && (
                  <p id={`faq-panel-${index}`} aria-labelledby={`faq-btn-${index}`} className="px-6 pb-5 text-sm leading-relaxed text-muted animate-fade">
                    {faq.a}
                  </p>
                )}
              </div>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}

// ── CTA final ───────────────────────────────────────────────────────────────

function FinalCTA() {
  return (
    <section className="mx-auto max-w-6xl px-5 pb-20 sm:px-6">
      {/* Card "inversa" de firma: usa neutros FIJOS (#101518 / #f2f5f4) en vez
          de los tokens del tema, porque `ink`/`paper` se invierten en modo oscuro
          y la card quedaba clara sobre fondo oscuro (al revés del tema). */}
      <Reveal className="relative overflow-hidden rounded-[28px] bg-[#101518] px-6 py-16 text-center text-[#f2f5f4] sm:px-12">
        <div className="absolute inset-0 [background-image:radial-gradient(rgba(242,245,244,0.14)_1px,transparent_1px)] opacity-100 [mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]" />
        <div
          aria-hidden="true"
          className="absolute -left-10 -top-12 size-52 rounded-full bg-lime/20 blur-3xl animate-drift-slow"
        />
        <div
          aria-hidden="true"
          className="absolute -bottom-16 -right-8 size-56 rounded-full bg-teal/25 blur-3xl animate-drift-slow"
          style={{ animationDelay: "2s" }}
        />
        <span
          aria-hidden="true"
          className="absolute left-6 top-6 hidden rotate-[-8deg] rounded-full border border-[#f2f5f4]/25 px-3 py-1 text-[11px] font-extrabold uppercase tracking-wider text-[#f2f5f4]/75 sm:block animate-drift"
          style={{ "--drift-rotate": "-8deg" }}
        >
          sin tarjeta
        </span>
        <div className="relative mx-auto max-w-2xl">
          <Palette size={32} className="mx-auto mb-5 text-lime" aria-hidden="true" />
          <h2 className="text-[clamp(26px,4vw,42px)] font-extrabold leading-tight">
            Empezá a recibir huéspedes hoy
          </h2>
          <p className="mx-auto mt-4 max-w-md text-[#f2f5f4]/70">
            Creá tu cuenta, personalizá tu alojamiento y compartí el enlace. Sin tarjeta.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link to="/auth/registro" className={buttonClass("primary", "min-h-[52px] px-7")}>
              Crear cuenta gratis <ArrowRight size={18} />
            </Link>
            <Link to="/auth/login" className="inline-flex min-h-[52px] items-center rounded-xl border border-[#f2f5f4]/25 px-6 text-sm font-bold text-[#f2f5f4] transition hover:bg-[#f2f5f4]/10">
              Ya tengo cuenta
            </Link>
          </div>
        </div>
      </Reveal>
    </section>
  );
}

// ── Footer ──────────────────────────────────────────────────────────────────

function Footer() {
  return (
    <footer className="relative overflow-hidden border-t border-line py-12">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 bottom-0 h-40 opacity-[0.06]"
        style={{
          backgroundImage:
            "repeating-linear-gradient(-45deg, var(--color-ink) 0 1px, transparent 1px 12px)",
        }}
      />
      <div className="relative mx-auto max-w-6xl px-5 sm:px-6">
        <div className="flex flex-col items-center justify-between gap-8 md:flex-row md:items-start">
          <div className="grid justify-items-center gap-3 md:justify-items-start">
            <Brand />
            <p className="max-w-xs text-center text-sm text-muted md:text-left">
              Gestión simple para alojamientos del Delta del Tigre.
            </p>
          </div>
          <nav className="flex flex-wrap justify-center gap-x-8 gap-y-3 text-sm font-semibold text-ink-soft">
            <a href="#caracteristicas" className="transition-colors hover:text-ink">Características</a>
            <a href="#precios" className="transition-colors hover:text-ink">Precios</a>
            <Link to="/auth/login" className="transition-colors hover:text-ink">Ingresar</Link>
            <Link to="/auth/registro" className="inline-flex items-center gap-1 transition-colors hover:text-ink">
              Crear cuenta <ArrowUpRight size={14} />
            </Link>
          </nav>
        </div>
        <div className="mt-10 flex flex-col items-center justify-between gap-3 border-t border-line pt-6 text-xs text-muted md:flex-row">
          <p>© 2026 HostFlow · NoCountry</p>
          <p className="inline-flex items-center gap-1.5">
            Hecho con dedicación en el Delta del Tigre <Sparkles size={13} className="text-teal-strong" aria-hidden="true" />
          </p>
        </div>
      </div>
    </footer>
  );
}

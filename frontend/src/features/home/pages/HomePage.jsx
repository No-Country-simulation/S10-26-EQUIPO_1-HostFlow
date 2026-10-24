import { useEffect, useState } from "react";
import { Link } from "react-router";
import {
  ArrowRight,
  ArrowUpRight,
  BedDouble,
  CalendarCheck,
  Check,
  ChevronDown,
  CreditCard,
  Menu,
  Palette,
  ShieldCheck,
  Sparkles,
  Star,
  X,
} from "lucide-react";
import { Brand, buttonClass, DemoBanner, ThemeToggle } from "@components/ui";
import { cn } from "@lib/format";

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
      <Features />
      <HowItWorks />
      <Stats />
      <Pricing />
      <FAQ />
      <FinalCTA />
      <Footer />
    </div>
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
          <a href="#caracteristicas" className="rounded-lg px-4 py-2 text-sm font-semibold text-ink-soft transition-colors hover:text-ink">
            Características
          </a>
          <a href="#como-funciona" className="rounded-lg px-4 py-2 text-sm font-semibold text-ink-soft transition-colors hover:text-ink">
            Cómo funciona
          </a>
          <a href="#precios" className="rounded-lg px-4 py-2 text-sm font-semibold text-ink-soft transition-colors hover:text-ink">
            Precios
          </a>
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
    <section className="relative overflow-hidden px-5 pt-16 pb-16 sm:px-6 lg:pt-24 lg:pb-24">
      <div className="pointer-events-none absolute inset-0 -z-10" aria-hidden="true">
        <div className="absolute -top-24 right-[-10%] size-[520px] rounded-full bg-lime/35 blur-[130px]" />
        <div className="absolute bottom-[-30%] left-[-10%] size-[460px] rounded-full bg-teal/20 blur-[130px]" />
        <div className="absolute inset-0 dot-grid opacity-40 [mask-image:radial-gradient(ellipse_at_center,black,transparent_72%)]" />
      </div>

      <div className="mx-auto max-w-6xl">
        <div className="grid items-center gap-14 lg:grid-cols-[1.05fr_0.95fr]">
          <div className="animate-rise">
            <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-line bg-paper px-4 py-1.5 text-xs font-bold text-ink-soft shadow-card">
              <span className="size-2 rounded-full bg-teal" />
              Nuevo · Gestión de alojamientos del Delta
            </span>

            <h1 className="text-[clamp(38px,6.4vw,68px)] font-extrabold leading-[1.02] tracking-[-0.035em]">
              Recibí a tus huéspedes con{" "}
              <span className="text-gradient-lime">fluidez</span>
            </h1>

            <p className="mt-6 max-w-lg text-lg leading-relaxed text-muted">
              HostFlow centraliza reservas, adicionales y disponibilidad en una plataforma
              minimalista, diseñada para alojamientos que quieren profesionalizar su gestión.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-4">
              <Link to="/auth/registro" className={buttonClass("primary", "min-h-[54px] px-7 text-[15px]")}>
                Comenzar gratis <ArrowRight size={18} />
              </Link>
              <a href="#como-funciona" className="inline-flex items-center gap-2 text-sm font-bold text-ink-soft transition-colors hover:text-ink">
                Ver cómo funciona <ArrowRight size={15} />
              </a>
            </div>

            <div className="mt-10 flex items-center gap-4">
              <div className="flex -space-x-2.5">
                {["M", "L", "C", "J"].map((initial, index) => (
                  <span
                    key={initial}
                    className={cn(
                      "grid size-10 place-items-center rounded-full border-2 border-paper text-xs font-extrabold",
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

          {/* Visual */}
          <div className="relative animate-rise">
            <HeroCard />
            <div className="absolute -bottom-5 -left-3 animate-float rounded-2xl border border-line bg-paper px-4 py-3 shadow-pop sm:-left-6">
              <div className="flex items-center gap-3">
                <span className="grid size-9 place-items-center rounded-xl bg-lime text-lime-ink">
                  <Sparkles size={17} />
                </span>
                <div>
                  <p className="text-xs font-extrabold text-ink">Respuesta inmediata</p>
                  <p className="text-[11px] text-muted">Confirmación en tiempo real</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function HeroCard() {
  const rows = [
    { icon: Check, title: "Check-in completado", text: "María G. · 3 huéspedes · Mañana 14:00", tone: "bg-lime text-lime-ink", badge: "Confirmado", badgeTone: "bg-lime-soft text-lime-ink" },
    { icon: BedDouble, title: "Nueva reserva", text: "Lucas F. · 2 noches · USD 340", tone: "bg-teal-soft text-teal-strong", badge: "Pendiente", badgeTone: "bg-pink-soft text-pink-strong" },
    { icon: CreditCard, title: "Cobro recibido", text: "Mercado Pago · $255.000", tone: "bg-pink-soft text-pink-strong", badge: "Pagado", badgeTone: "bg-success-soft text-success" },
  ];

  return (
    <div className="relative rounded-[28px] border border-line bg-paper p-5 shadow-pop sm:p-6">
      <div className="mb-5 flex items-center justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-wide text-muted">Panel</p>
          <p className="font-extrabold text-ink">Cabañas del Arroyo</p>
        </div>
        <span className="rounded-full bg-lime px-3 py-1 text-[11px] font-extrabold text-lime-ink">Hoy</span>
      </div>
      <div className="grid gap-3">
        {rows.map((row) => (
          <div key={row.title} className="flex items-center gap-3 rounded-2xl border border-line bg-cloud p-3.5">
            <span className={cn("grid size-10 shrink-0 place-items-center rounded-xl", row.tone)}>
              <row.icon size={18} />
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-ink">{row.title}</p>
              <p className="truncate text-xs text-muted">{row.text}</p>
            </div>
            <span className={cn("shrink-0 rounded-full px-2.5 py-1 text-[10px] font-extrabold", row.badgeTone)}>
              {row.badge}
            </span>
          </div>
        ))}
      </div>
      <div className="mt-5 grid grid-cols-3 gap-3 border-t border-line pt-5 text-center">
        {[
          ["12", "Reservas"],
          ["4", "Habitaciones"],
          ["$1,2M", "Ingresos"],
        ].map(([value, label]) => (
          <div key={label}>
            <p className="text-lg font-extrabold text-ink">{value}</p>
            <p className="text-[11px] text-muted">{label}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Marquee ─────────────────────────────────────────────────────────────────

function LogoMarquee() {
  const names = ["Cabañas del Río", "Posada Delta", "Albergue La Lancha", "Hotel Tigre", "Lo del Mono", "Mirador del Paraná", "Casa Flotante"];
  const doubled = [...names, ...names];

  return (
    <section className="border-y border-line py-8">
      <p className="mb-6 text-center text-xs font-extrabold uppercase tracking-[0.18em] text-muted">
        Confían en HostFlow
      </p>
      <div className="relative overflow-hidden [mask-image:linear-gradient(90deg,transparent,black_12%,black_88%,transparent)]">
        <div className="flex w-max animate-marquee gap-10 pr-10">
          {doubled.map((name, index) => (
            <span key={`${name}-${index}`} className="flex items-center gap-2 whitespace-nowrap text-sm font-bold text-ink-soft/70">
              <span className="grid size-6 place-items-center rounded-md bg-lime text-[10px] font-extrabold text-lime-ink">H</span>
              {name}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}

// ── Features ────────────────────────────────────────────────────────────────

function Features() {
  const tones = { lime: "bg-lime text-lime-ink", teal: "bg-teal-soft text-teal-strong", pink: "bg-pink-soft text-pink-strong" };
  return (
    <section id="caracteristicas" className="mx-auto max-w-6xl px-5 py-20 sm:px-6 lg:py-28">
      <div className="max-w-2xl animate-rise">
        <span className="mb-3 inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-['']">
          Características
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
          Todo lo que necesitás, <span className="text-gradient-lime">nada que sobre</span>
        </h2>
        <p className="mt-4 text-lg leading-relaxed text-muted">
          Herramientas pensadas para pequeños y medianos alojamientos que quieren crecer sin complicarse.
        </p>
      </div>

      <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {features.map((feature, index) => (
          <article key={feature.title} className="card card-hover group p-6" style={{ animationDelay: `${index * 80}ms` }}>
            <span className={cn("mb-5 grid size-12 place-items-center rounded-2xl transition-transform duration-300 group-hover:-translate-y-1 group-hover:scale-110", tones[feature.tone])}>
              <feature.icon size={22} />
            </span>
            <h3 className="mb-2 font-bold text-ink">{feature.title}</h3>
            <p className="text-sm leading-relaxed text-muted">{feature.text}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

// ── How it works ────────────────────────────────────────────────────────────

function HowItWorks() {
  return (
    <section id="como-funciona" className="border-y border-line bg-cloud py-20 lg:py-28">
      <div className="mx-auto max-w-6xl px-5 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
            Cómo funciona
          </span>
          <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">Simple desde el primer día</h2>
        </div>

        <div className="mt-16 grid gap-8 md:grid-cols-3">
          {steps.map((step, index) => (
            <div key={step.title} className="relative text-center">
              {index < steps.length - 1 && (
                <span className="absolute left-[calc(50%+42px)] top-10 hidden h-px w-[calc(100%-84px)] bg-line-strong md:block" aria-hidden="true" />
              )}
              <span className="relative mx-auto mb-6 grid size-20 place-items-center rounded-3xl border border-line bg-paper text-2xl font-extrabold text-ink shadow-card">
                {index + 1}
                <span className="absolute -right-1.5 -top-1.5 grid size-6 place-items-center rounded-full bg-lime text-[11px] font-extrabold text-lime-ink">
                  ✓
                </span>
              </span>
              <h3 className="mb-2 text-lg font-bold text-ink">{step.title}</h3>
              <p className="mx-auto max-w-xs text-sm leading-relaxed text-muted">{step.text}</p>
            </div>
          ))}
        </div>

        <div className="mt-14 flex justify-center">
          <Link to="/auth/registro" className={buttonClass("primary", "min-h-[52px] px-7")}>
            Empezar ahora <ArrowRight size={18} />
          </Link>
        </div>
      </div>
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
    <section className="mx-auto max-w-6xl px-5 py-20 sm:px-6">
      <div className="rounded-[28px] border border-line bg-gradient-to-r from-lime-soft via-teal-soft to-pink-soft px-8 py-12">
        <div className="grid grid-cols-2 gap-8 md:grid-cols-4">
          {stats.map((stat) => (
            <div key={stat.label} className="text-center">
              <p className="text-[clamp(30px,4vw,44px)] font-extrabold leading-none text-ink">{stat.value}</p>
              <p className="mt-2 text-sm font-semibold text-ink-soft">{stat.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ── Pricing ─────────────────────────────────────────────────────────────────

function Pricing() {
  return (
    <section id="precios" className="border-y border-line bg-cloud py-20 lg:py-28">
      <div className="mx-auto max-w-5xl px-5 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
            Precios
          </span>
          <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">
            Todas las características incluidas
          </h2>
          <p className="mt-4 text-lg text-muted">Sin compromiso. Cancelá cuando quieras.</p>
        </div>

        <div className="mt-14 grid gap-6 md:grid-cols-2">
          {plans.map((plan) => (
            <article
              key={plan.name}
              className={cn(
                "card relative overflow-hidden p-8",
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
      <div className="mb-12 text-center">
        <span className="mb-3 inline-flex items-center justify-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-[''] after:h-0.5 after:w-4 after:rounded-sm after:bg-current after:content-['']">
          FAQ
        </span>
        <h2 className="text-[clamp(28px,4vw,44px)] font-extrabold leading-[1.08]">Preguntas frecuentes</h2>
      </div>

      <div className="grid gap-3">
        {faqs.map((faq, index) => {
          const isOpen = open === index;
          return (
            <div key={faq.q} className="card overflow-hidden">
              <button onClick={() => setOpen(isOpen ? -1 : index)} className="flex w-full cursor-pointer items-center justify-between gap-4 px-6 py-4 text-left">
                <span className="font-bold text-ink">{faq.q}</span>
                <ChevronDown size={18} className={cn("shrink-0 text-muted transition-transform duration-300", isOpen && "rotate-180")} />
              </button>
              {isOpen && <p className="px-6 pb-5 text-sm leading-relaxed text-muted animate-fade">{faq.a}</p>}
            </div>
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
      <div className="relative overflow-hidden rounded-[28px] bg-ink px-6 py-16 text-center text-paper sm:px-12">
        <div className="absolute inset-0 dot-grid opacity-15 [mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]" />
        <div className="relative mx-auto max-w-2xl">
          <Palette size={32} className="mx-auto mb-5 text-lime" />
          <h2 className="text-[clamp(26px,4vw,42px)] font-extrabold leading-tight">
            Empezá a recibir huéspedes hoy
          </h2>
          <p className="mx-auto mt-4 max-w-md text-paper/70">
            Creá tu cuenta, personalizá tu alojamiento y compartí el enlace. Sin tarjeta.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link to="/auth/registro" className={buttonClass("primary", "min-h-[52px] px-7")}>
              Crear cuenta gratis <ArrowRight size={18} />
            </Link>
            <Link to="/auth/login" className="inline-flex min-h-[52px] items-center rounded-xl border border-paper/25 px-6 text-sm font-bold text-paper transition hover:bg-paper/10">
              Ya tengo cuenta
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

// ── Footer ──────────────────────────────────────────────────────────────────

function Footer() {
  return (
    <footer className="border-t border-line py-12">
      <div className="mx-auto max-w-6xl px-5 sm:px-6">
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
          <p>Hecho con dedicación en el Delta del Tigre</p>
        </div>
      </div>
    </footer>
  );
}

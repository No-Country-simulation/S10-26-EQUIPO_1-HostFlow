import { ArrowRight, ClipboardCheck, CreditCard, Package, Sailboat, Waves } from "lucide-react";
import { Link } from "react-router";
import { Brand, buttonClass, DemoBanner, Eyebrow } from "../components/ui";

const features = [
  {
    icon: ClipboardCheck,
    title: "Check-in sin vueltas",
    text: "Tus huéspedes completan sus datos de llegada desde un enlace, sin crear cuenta ni instalar nada.",
  },
  {
    icon: Package,
    title: "Adicionales a medida",
    text: "Ofrecé desayunos, excursiones o lo que quieras y recibí los pedidos ordenados en un solo lugar.",
  },
  {
    icon: Sailboat,
    title: "Traslados en lancha",
    text: "El huésped solicita su traslado al reservar y vos ves cada solicitud sin perseguir mensajes.",
  },
  {
    icon: CreditCard,
    title: "Medios de pago a la vista",
    text: "Compartí tus datos de cobro de forma clara. HostFlow no procesa pagos: solo los muestra.",
  },
];

const steps = [
  ["1", "Creá la cuenta de tu alojamiento", "Cargá su nombre y tus datos en pocos minutos."],
  ["2", "Compartí tu enlace", "Cada huésped recibe un acceso propio para completar su llegada."],
  [
    "3",
    "Recibilos con todo listo",
    "Datos, adicionales y traslados organizados antes de que lleguen.",
  ],
];

const WRAP = "mx-auto w-[min(100%_-_48px,1140px)] max-[600px]:w-[min(100%_-_32px,1140px)]";

const SECTION_TITLE =
  "mt-2.5 max-w-160 text-[clamp(28px,3.4vw,40px)] font-medium tracking-[-0.02em]";

const CARD_TEXT = "text-[15px] leading-[1.6] text-muted";

export default function Home() {
  return (
    <div className="min-h-screen bg-paper text-ink">
      <DemoBanner />

      <header className={`${WRAP} flex items-center justify-between py-5.5`}>
        <Brand />

        <nav className="flex gap-2.5">
          <Link className={buttonClass("secondary")} to="/auth/login">
            Ingresar
          </Link>

          <Link className={buttonClass("primary", "max-[600px]:hidden")} to="/auth/registro">
            Crear cuenta
          </Link>
        </nav>
      </header>

      <section
        className={`${WRAP} grid grid-cols-[1.1fr_0.9fr] items-center gap-10 pb-18 pt-12 max-[800px]:grid-cols-1 max-[800px]:pb-12`}
      >
        <div>
          <Eyebrow>Gestión simple. Hospitalidad real.</Eyebrow>

          <h1 className="mb-5.5 mt-3.5 text-[clamp(38px,5vw,64px)] font-medium leading-[1.06] tracking-[-0.03em]">
            Tu alojamiento del Delta,
            <br />
            más cerca de cada huésped.
          </h1>

          <p className="max-w-130 text-lg leading-[1.7] text-ink-soft">
            HostFlow centraliza el check-in, los adicionales y los traslados en una experiencia
            pensada para pequeños alojamientos del Delta del Tigre.
          </p>

          <div className="mt-8 flex flex-wrap gap-3">
            <Link className={buttonClass("primary")} to="/auth/registro">
              Crear cuenta de alojamiento <ArrowRight size={18} />
            </Link>

            <Link className={buttonClass("secondary")} to="/auth/login">
              Ya tengo cuenta
            </Link>
          </div>
        </div>

        <div
          className="relative isolate min-h-95 overflow-hidden rounded-[26px] bg-story shadow-md before:absolute before:inset-0 before:-z-10 before:bg-wave before:bg-size-[140px_28px] before:content-[''] max-[800px]:min-h-75"
          aria-hidden="true"
        >
          <Waves className="absolute -bottom-32.5 -right-30 -z-10 size-110 opacity-[0.07]" />

          <HomeCard
            className="left-[8%] top-[12%]"
            label="Check-in completo"
            title="Martina y 2 huéspedes más"
            detail="Llegan mañana · Lancha confirmada"
          />

          <HomeCard
            className="bottom-[12%] right-[8%]"
            label="Adicional pedido"
            title="Desayuno en la cabaña"
            detail="Para 3 personas"
          />
        </div>
      </section>

      <section className={`${WRAP} py-14`}>
        <Eyebrow>Todo en un lugar</Eyebrow>

        <h2 className={`${SECTION_TITLE} mb-8`}>Menos mensajes, más tiempo para recibir.</h2>

        <div className="grid grid-cols-4 gap-4 min-[601px]:max-[1050px]:grid-cols-2 max-[600px]:grid-cols-1">
          {features.map(({ icon: Icon, title, text }) => (
            <article
              className="rounded-[22px] border border-line bg-white px-6 py-6.5 shadow-xs"
              key={title}
            >
              <span className="mb-4.5 grid size-11.5 place-items-center rounded-[14px] bg-green-soft text-green">
                <Icon className="size-5.5" />
              </span>

              <h3 className="mb-2 text-lg">{title}</h3>
              <p className={CARD_TEXT}>{text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className={`${WRAP} py-14`}>
        <Eyebrow>Cómo funciona</Eyebrow>

        <h2 className={`${SECTION_TITLE} mb-8`}>Tres pasos para empezar.</h2>

        <ol className="m-0 grid list-none grid-cols-3 gap-4 p-0 max-[800px]:grid-cols-1">
          {steps.map(([n, title, text]) => (
            <li className="flex gap-4 rounded-[22px] bg-sand p-6" key={n}>
              <b className="grid size-9.5 shrink-0 place-items-center rounded-full bg-green font-display font-bold text-white">
                {n}
              </b>

              <div>
                <h3 className="mb-1.5 text-[17px]">{title}</h3>
                <p className={CARD_TEXT}>{text}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section
        className={`${WRAP} mb-14 mt-8 rounded-[26px] bg-brand px-10 py-14 text-center text-white shadow-md max-[600px]:px-5.5 max-[600px]:py-10`}
      >
        <h2 className={`${SECTION_TITLE} mx-auto mb-7`}>
          Empezá a recibir a tus huéspedes de otra manera.
        </h2>

        <Link className={buttonClass("primary")} to="/auth/registro">
          Crear mi cuenta <ArrowRight size={18} />
        </Link>
      </section>

      <footer
        className={`${WRAP} flex items-center justify-between border-t border-line pb-10 pt-6 text-muted max-[600px]:flex-col max-[600px]:gap-2.5`}
      >
        <Brand />
        <small>© NoCountry · HostFlow</small>
      </footer>
    </div>
  );
}

// Tarjeta de ejemplo de la portada (ilustrativa).
function HomeCard({ className, label, title, detail }) {
  return (
    <div
      className={`absolute grid w-[min(78%,300px)] gap-1 rounded-[18px] bg-white px-5 py-4.5 shadow-lg ${className}`}
    >
      <span className="text-xs font-bold uppercase tracking-[0.06em] text-green">{label}</span>

      <strong className="text-base">{title}</strong>
      <small className="text-muted">{detail}</small>
    </div>
  );
}

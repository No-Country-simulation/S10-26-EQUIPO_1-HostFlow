import { ArrowRight, ClipboardCheck, CreditCard, Package, Sailboat, Waves } from "lucide-react"
import { Link } from "react-router"
import { Brand, DemoBanner } from "../components/ui"

const features = [
  {
    icon: ClipboardCheck,
    title: "Check-in sin vueltas",
    text: "Tus huéspedes completan sus datos de llegada desde un enlace, sin crear cuenta ni instalar nada.",
  },
  {
    icon: Package,
    title: "Adicionales a medida",
    text: "Ofreceles  desayunos, excursiones o lo que quieras y recibí los pedidos ordenados en un solo lugar.",
  },
  {
    icon: Sailboat,
    title: "Traslados en lancha",
    text: "El huésped solicita su traslado al reservar y vos ves cada solicitud sin perseguir mensajes.",
  },
  {
    icon: CreditCard,
    title: "Medios de pago a la vista",
    text: "Comparte tus datos de cobro de forma clara. HostFlow no procesa pagos: solo los muestra.",
  },
]

const steps = [
  ["1", "Crea la cuenta de tu alojamiento", "Cargá su nombre y tus datos en pocos minutos."],
  ["2", "Comparte tu enlace", "Cada huésped recibe un acceso propio para completar su llegada."],
  ["3", "Recibelos con todo listo", "Datos, adicionales y traslados organizados antes de que lleguen."],
]

export default function Home() {
  return (
    <div className="home">
      <DemoBanner />
      <header className="home-nav">
        <Brand />
        <nav>
          <Link className="btn btn-secondary" to="/auth/login">
            Ingresar
          </Link>
          <Link className="btn btn-primary home-nav-cta" to="/auth/registro">
            Crear cuenta
          </Link>
        </nav>
      </header>

      <section className="home-hero">
        <div className="home-hero-copy">
          <span className="eyebrow">Gestión simple. Hospitalidad real.</span>
          <h1>
            Tu alojamiento del Delta,
            <br />
            más cerca de cada huésped.
          </h1>
          <p>
            HostFlow centraliza el check-in, los adicionales y los traslados en una experiencia
            pensada para pequeños alojamientos del Delta del Tigre.
          </p>
          <div className="home-actions">
            <Link className="btn btn-primary" to="/auth/registro">
              Crear cuenta de alojamiento <ArrowRight size={18} />
            </Link>
            <Link className="btn btn-secondary" to="/auth/login">
              Ya tengo cuenta
            </Link>
          </div>
        </div>
        <div className="home-hero-art" aria-hidden="true">
          <Waves className="story-wave" />
          <div className="home-card">
            <span>Check-in completo</span>
            <strong>Martina y 2 huéspedes más</strong>
            <small>Llegan mañana · Lancha confirmada</small>
          </div>
          <div className="home-card home-card-b">
            <span>Adicional pedido</span>
            <strong>Desayuno en la cabaña</strong>
            <small>Para 3 personas</small>
          </div>
        </div>
      </section>

      <section className="home-section">
        <span className="eyebrow">Todo en un lugar</span>
        <h2>Menos mensajes, más tiempo para recibir.</h2>
        <div className="home-grid">
          {features.map(({ icon: Icon, title, text }) => (
            <article className="home-feature" key={title}>
              <span>
                <Icon />
              </span>
              <h3>{title}</h3>
              <p>{text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="home-section home-steps">
        <span className="eyebrow">Cómo funciona</span>
        <h2>Tres pasos para empezar.</h2>
        <ol>
          {steps.map(([n, title, text]) => (
            <li key={n}>
              <b>{n}</b>
              <div>
                <h3>{title}</h3>
                <p>{text}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className="home-cta">
        <h2>Empezá a recibir a tus huéspedes de otra manera.</h2>
        <Link className="btn btn-primary" to="/auth/registro">
          Crear mi cuenta <ArrowRight size={18} />
        </Link>
      </section>

      <footer className="home-footer">
        <Brand />
        <small>© NoCountry · HostFlow</small>
      </footer>
    </div>
  )
}

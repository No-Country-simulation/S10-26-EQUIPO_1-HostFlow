import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  ClipboardList,
  Clock,
  Copy,
  CreditCard,
  HeartPulse,
  IdCard,
  Landmark,
  Mail,
  MessageSquare,
  Phone,
  ShieldCheck,
  Ship,
  Sparkles,
  UserRound,
  Wallet,
} from "lucide-react";
import { Alert, Badge, Button, EmptyState, Field, WhatsAppIcon } from "@components/ui";
import OnboardingShell from "../components/OnboardingShell";
import OnboardingIntro from "../components/OnboardingIntro";
import BoatQuoteStep from "../components/BoatQuoteStep";
import StayCard from "../components/StayCard";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { isOnboardingLinkExpired } from "@lib/state/store";
import { DEMO_MODE } from "@lib/config/env";
import { bookingGrandTotal, cn, copyText, daysBetween, formatDate, formatMoney, isEmail, plural, serviceLineDetail, serviceLineTotal } from "@utils/format";

const STEPS = [
  { id: 0, label: "Tus datos", icon: UserRound },
  { id: 1, label: "Alérgenos y preferencias", icon: HeartPulse },
  { id: 2, label: "Servicios", icon: ClipboardList },
  { id: 3, label: "Traslado", icon: Ship },
  { id: 4, label: "Confirmar", icon: Check },
];

const EMPTY_QUOTE = {
  decided: false,
  wanted: false,
  pickupPlace: "",
  pickupDate: "",
  pickupTime: "",
  passengers: "2",
  baggage: "0",
  dropoffPlace: "",
  notes: "",
};

// Tope de unidades para un servicio de precio fijo (el huésped elige cuántos).
const MAX_SERVICE_QTY = 20;

// Validación del paso de datos. Devuelve un mensaje por campo (vacío si está
// bien) para mostrarlo debajo de cada input, no en un aviso único al final.
const GUEST_FIELDS = ["guestName", "guestDocument", "guestEmail", "guestPhone"];

function validateGuest(guest) {
  const errors = {};

  // Nombre y apellido: se validan como un solo dato y toleran espacios de más.
  const name = guest.guestName.trim().replace(/\s+/g, " ");
  if (!name) errors.guestName = "Ingresá tu nombre y apellido.";
  else if (name.length < 3) errors.guestName = "Escribí tu nombre completo.";
  else if (!/^[\p{L}][\p{L}\s.'’-]*$/u.test(name)) {
    errors.guestName = "Usá solo letras, espacios, puntos o guiones.";
  } else if (!name.includes(" ")) errors.guestName = "Agregá también tu apellido.";

  // Documento: DNI, CI o pasaporte (5 a 20 caracteres).
  const document = guest.guestDocument.trim().replace(/\s+/g, " ");
  if (!document) errors.guestDocument = "Ingresá tu DNI o pasaporte.";
  else if (!/^[0-9A-Za-z][0-9A-Za-z.\- ]{4,19}$/.test(document)) {
    errors.guestDocument = "Entre 5 y 20 caracteres: letras, números, puntos o guiones.";
  }

  const email = guest.guestEmail.trim();
  if (!email) errors.guestEmail = "Ingresá tu correo electrónico.";
  else if (!isEmail(email)) errors.guestEmail = "Ese correo no parece válido. Revisalo.";

  // Teléfono: obligatorio, para poder coordinar la llegada.
  const phone = guest.guestPhone.trim();
  if (!phone) errors.guestPhone = "Ingresá tu teléfono.";
  else {
    const digits = phone.replace(/\D/g, "");
    if (!/^[+0-9()\s.-]+$/.test(phone) || digits.length < 6 || digits.length > 15) {
      errors.guestPhone = "Revisá el número: entre 6 y 15 dígitos.";
    }
  }

  return errors;
}

// Limpia espacios de más antes de guardar (el backend recibe el dato prolijo).
function cleanGuest(guest) {
  return {
    ...guest,
    guestName: guest.guestName.trim().replace(/\s+/g, " "),
    guestDocument: guest.guestDocument.trim().replace(/\s+/g, " "),
    guestEmail: guest.guestEmail.trim(),
    guestPhone: guest.guestPhone.trim(),
  };
}

// Encabezado de cada paso: ícono en color de marca + etiqueta + título. Es
// presentación (no cambia el flujo) y hereda la identidad del alojamiento.
function StepHeading({ icon: Icon, eyebrow, title, text, aside }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div className="flex min-w-0 items-start gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-[var(--radius-chip)] bg-teal-soft text-teal-strong">
          <Icon size={19} aria-hidden="true" />
        </span>
        <div className="min-w-0">
          <p className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-teal-strong">{eyebrow}</p>
          <h1 className="mt-0.5 text-xl font-extrabold text-ink">{title}</h1>
          {text && <p className="mt-1 text-sm text-muted">{text}</p>}
        </div>
      </div>
      {aside}
    </div>
  );
}

// Onboarding B2C: el huésped accede con el enlace único de su reserva
// (/llegada/:lodgingId/:bookingId), sin crear cuenta. La identidad visual es la
// del alojamiento (OnboardingShell + LodgingTheme).
export default function OnboardingPage() {
  const { lodgingId, bookingId } = useParams();
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const booking = useStore((s) => s.bookings.find((b) => b.id === bookingId));
  const services = useStore((s) => s.services);
  const payments = useStore((s) => s.payments);
  const bankDetails = payments?.bankDetails || {};
  const paymentMethods = payments?.methods || [];

  const [step, setStep] = useState(0);
  const [started, setStarted] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [copied, setCopied] = useState(false);
  const [copiedMethodId, setCopiedMethodId] = useState(null);

  const [guest, setGuest] = useState({
    guestName: booking?.guestName || "",
    guestDocument: "",
    guestEmail: booking?.guestEmail || "",
    guestPhone: booking?.guestPhone || "",
  });
  const [health, setHealth] = useState({ allergies: "", preferences: "", notes: "" });
  const [selected, setSelected] = useState([]); // [{ serviceId, name, price }]
  const [quote, setQuote] = useState(EMPTY_QUOTE);

  // El huésped abre el enlace desde su propio dispositivo, donde no hay datos
  // locales: se piden al backend (endpoint público, sin token) el alojamiento,
  // la reserva, los servicios y las formas de pago con sus credenciales.
  // En modo demo todo ya vive en el store del navegador.
  useEffect(() => {
    if (DEMO_MODE || !lodgingId || !bookingId) return;
    let alive = true;
    Promise.resolve()
      .then(() => actions.getOnboarding?.(lodgingId, bookingId))
      .catch((err) => {
        if (alive) console.warn("Onboarding:", err);
      });
    return () => {
      alive = false;
    };
  }, [actions, lodgingId, bookingId]);

  const linkValid = lodgingId === lodging.id;
  const availableServices = useMemo(() => services.filter((sv) => sv.enabled), [services]);
  // Fotos del alojamiento (0 a 4): una por paso, en el mismo orden del panel.
  const stepPhotos = Array.isArray(lodging.images) ? lodging.images : [];
  const nights = booking ? daysBetween(booking.checkIn, booking.checkOut) : 0;
  // Huéspedes de la reserva: se usan en los servicios que se cobran por persona.
  const guests = Math.max(1, Number(booking?.guests) || 1);
  // Total REAL: alojamiento + servicios (por noche × noches, por huésped, fijos × cantidad).
  const servicesTotal = selected.reduce((sum, item) => sum + serviceLineTotal(item, nights, guests), 0);
  const orderTotal = (Number(booking?.total) || 0) + servicesTotal;
  const savedTotal = booking ? bookingGrandTotal(booking) : orderTotal;
  const hasBankDetails = Boolean(bankDetails.holder || bankDetails.alias || bankDetails.cbu);
  const activePaymentMethods = useMemo(
    () => (Array.isArray(paymentMethods) ? paymentMethods.filter((m) => m.enabled) : []),
    [paymentMethods],
  );

  const copyMethodCredentials = async (method) => {
    const keys = method.keys || {};
    const lines = [`Método de pago: ${method.label}`];
    if (keys.holder) lines.push(`Titular: ${keys.holder}`);
    if (keys.alias) lines.push(`Alias: ${keys.alias}`);
    if (keys.cbu) lines.push(`CBU: ${keys.cbu}`);
    if (keys.cvu) lines.push(`CVU: ${keys.cvu}`);
    if (keys.bank) lines.push(`Banco: ${keys.bank}`);
    if (keys.email) lines.push(`Correo: ${keys.email}`);
    if (keys.paypalMe) lines.push(`PayPal.Me: ${keys.paypalMe}`);
    if (keys.publicKey) lines.push(`Llave pública: ${keys.publicKey}`);
    if (method.notes) lines.push(`Instrucciones: ${method.notes}`);

    if (!(await copyText(lines.join("\n")))) return;
    setCopiedMethodId(method.id);
    setTimeout(() => setCopiedMethodId(null), 2500);
  };

  const completed = Boolean(booking?.onboarding?.completedAt) || done;

  // Vigencia del enlace de llegada (24 h) y estado del pago de la reserva.
  const linkExpired = booking ? isOnboardingLinkExpired(booking) : false;
  const paid = booking?.paymentStatus === "PAID";
  // WhatsApp de la empresa (solo frontend: wa.me con el teléfono del alojamiento).
  const waDigits = String(lodging.phone || "").replace(/\D/g, "");
  const waLink = (text) => `https://wa.me/${waDigits}?text=${encodeURIComponent(text)}`;
  const paymentMessage = booking
    ? `Hola ${lodging.name}, soy ${booking.guestName}. Te mando el comprobante de pago de mi reserva (${formatMoney(savedTotal, booking.currency)}).`
    : `Hola ${lodging.name}, te mando el comprobante de pago de mi reserva.`;

  const setGuestField = (key) => (event) => {
    const value = event.target.value;
    const nextGuest = { ...guest, [key]: value };
    setGuest(nextGuest);
    // Si el campo ya mostraba un error, se recalcula en vivo para que
    // desaparezca apenas se corrige (sin esperar a reintentar el paso).
    setFieldErrors((current) =>
      current[key] ? { ...current, [key]: validateGuest(nextGuest)[key] || "" } : current,
    );
  };
  // Al salir del campo se valida: el aviso aparece debajo del input.
  const blurGuestField = (key) => () => {
    const message = validateGuest(guest)[key] || "";
    setFieldErrors((current) => ({ ...current, [key]: message }));
  };
  const setHealthField = (key) => (event) => setHealth((h) => ({ ...h, [key]: event.target.value }));

  const toggleService = (service) => {
    setSelected((current) =>
      current.some((item) => item.serviceId === service.id)
        ? current.filter((item) => item.serviceId !== service.id)
        : [
            ...current,
            {
              serviceId: service.id,
              name: service.name,
              price: service.price,
              pricing: service.pricing === "PER_NIGHT" ? "PER_NIGHT" : "FIXED",
              perGuest: Boolean(service.perGuest),
              quantity: 1,
            },
          ],
    );
  };

  // Cantidad de un servicio de precio fijo (mínimo 1, máximo 20).
  const setQuantity = (serviceId, delta) => {
    setSelected((current) =>
      current.map((item) =>
        item.serviceId === serviceId
          ? { ...item, quantity: Math.min(MAX_SERVICE_QTY, Math.max(1, (Number(item.quantity) || 1) + delta)) }
          : item,
      ),
    );
  };

  const next = () => {
    setError("");
    if (step === 0) {
      const errors = validateGuest(guest);
      setFieldErrors(errors);
      const firstInvalid = GUEST_FIELDS.find((key) => errors[key]);
      if (firstInvalid) {
        document.querySelector(`[name="${firstInvalid}"]`)?.focus();
        return;
      }
      setGuest(cleanGuest(guest));
    }
    setStep((current) => Math.min(current + 1, STEPS.length - 1));
  };

  const back = () => {
    setError("");
    // En el primer paso, "Atrás" vuelve a la portada de presentación.
    if (step === 0) {
      setStarted(false);
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    setStep((current) => Math.max(current - 1, 0));
  };

  // Confirmación final: genera la solicitud de lancha (si la hubo) y guarda el
  // onboarding. Protegido contra dobles envíos (submitting / done).
  const confirm = () => {
    if (submitting || completed) return;

    // Última barrera: si el huésped volvió atrás y dejó algo mal, no se envía.
    const errors = validateGuest(guest);
    if (GUEST_FIELDS.some((key) => errors[key])) {
      setFieldErrors(errors);
      setStep(0);
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    setSubmitting(true);
    try {
      if (quote.decided && quote.wanted) {
        actions.createBoatQuoteRequest(booking.id, {
          guestName: guest.guestName,
          guestEmail: guest.guestEmail,
          guestPhone: guest.guestPhone,
          pickupPlace: quote.pickupPlace,
          pickupDate: quote.pickupDate,
          pickupTime: quote.pickupTime,
          passengers: quote.passengers,
          baggage: quote.baggage,
          dropoffPlace: quote.dropoffPlace || lodging.location,
          notes: quote.notes,
        });
      } else if (quote.decided) {
        actions.declineBoatQuote(booking.id);
      }
      actions.submitOnboarding(booking.id, {
        ...guest,
        ...health,
        selectedServices: selected,
        boatQuoteRequested: Boolean(quote.decided && quote.wanted),
      });
      setDone(true);
    } catch (err) {
      // Error real del backend: el huésped puede reintentar. Los módulos aún
      // no implementados caen solos al store local y no llegan acá.
      setError(err?.message || "No pudimos registrar tu llegada. Intentá nuevamente.");
    } finally {
      setSubmitting(false);
    }
  };

  const copyJson = async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(booking.boatQuote.payload, null, 2));
      setCopied(true);
      setTimeout(() => setCopied(false), 2200);
    } catch {
      setCopied(false);
    }
  };

  // ── Enlaces inválidos o reservas no disponibles ──────────────────────────
  if (!linkValid) {
    return (
      <OnboardingShell lodging={lodging}>
        <EmptyState
          icon={ClipboardList}
          title="Este enlace no es válido"
          text="El alojamiento del enlace no coincide. Pedile a tu anfitrión el enlace de llegada correcto."
          action={
            <Link to="/" className="mt-2 inline-flex text-sm font-semibold text-teal-strong hover:text-teal">
              Volver al inicio
            </Link>
          }
        />
      </OnboardingShell>
    );
  }

  if (!booking) {
    return (
      <OnboardingShell lodging={lodging}>
        <EmptyState
          icon={ClipboardList}
          title="No encontramos tu reserva"
          text="Este enlace de llegada ya no está disponible. Consultá con tu anfitrión."
          action={
            <Link to="/" className="mt-2 inline-flex text-sm font-semibold text-teal-strong hover:text-teal">
              Volver al inicio
            </Link>
          }
        />
      </OnboardingShell>
    );
  }

  // El enlace de llegada dura 24 h desde su emisión: si expiró y la ficha no
  // está completa, se pide un enlace nuevo por WhatsApp al alojamiento.
  if (!completed && linkExpired) {
    return (
      <OnboardingShell lodging={lodging}>
        <EmptyState
          icon={ClipboardList}
          title="Este enlace expiró"
          text={`Los enlaces de llegada duran 24 horas. ${lodging.name} te puede enviar uno nuevo: escribinos por WhatsApp (${lodging.phone}).`}
          action={
            <a
              href={waLink(`Hola ${lodging.name}, mi enlace de llegada expiró. ¿Me podés enviar uno nuevo?`)}
              target="_blank"
              rel="noreferrer"
              className="mt-2 inline-flex items-center gap-2 rounded-xl bg-lime px-4 py-2.5 text-sm font-bold text-lime-ink transition hover:opacity-90"
            >
              <WhatsAppIcon size={16} />
              Pedir enlace nuevo · {lodging.phone}
            </a>
          }
        />
      </OnboardingShell>
    );
  }

  if (booking.status === "CANCELLED") {
    return (
      <OnboardingShell lodging={lodging}>
        <EmptyState
          icon={ClipboardList}
          title="Esta reserva fue cancelada"
          text="Si creés que es un error, contactá a tu anfitrión."
        />
      </OnboardingShell>
    );
  }

  // ── Resumen final (tras confirmar, o si el onboarding ya estaba completo) ─
  if (completed) {
    const quotePayload = booking.boatQuote?.payload;
    return (
      <OnboardingShell lodging={lodging}>
        <div className="grid gap-5">
          <div className="card grid gap-3 overflow-hidden p-6 text-center">
            <span className="mx-auto grid size-14 place-items-center rounded-2xl bg-lime-soft text-lime-ink">
              <Check size={26} />
            </span>
            <h1 className="text-2xl font-extrabold text-ink">¡Llegada registrada!</h1>
            <p className="text-sm text-muted">
              Gracias, {booking.onboarding?.guestName || booking.guestName}. {lodging.name} ya tiene tus datos.
            </p>
            <div className="mt-1 flex flex-wrap justify-center gap-2">
              <Badge tone="teal">
                {formatDate(booking.checkIn)} · {plural(booking.guests, "huésped", "huéspedes")}
              </Badge>
              {paid && (
                <Badge tone="lime">
                  <Check size={11} /> Pago aceptado
                </Badge>
              )}
              {booking.boatQuote?.status === "REQUESTED" && (
                <Badge tone="pink">
                  <Ship size={11} /> Lancha: pendiente
                </Badge>
              )}
              {booking.boatQuote?.status === "DECLINED" && <Badge tone="neutral">Sin cotización de lancha</Badge>}
            </div>
          </div>

          {/* Estado del pago: "Pago aceptado" cuando el anfitrión lo confirma */}
          {paid ? (
            <div className="card grid gap-2 border border-lime/60 bg-lime-soft p-5 text-center">
              <span className="mx-auto grid size-12 place-items-center rounded-2xl bg-lime text-lime-ink">
                <Check size={24} />
              </span>
              <p className="text-lg font-extrabold text-ink">Pago aceptado</p>
              <p className="text-sm text-muted">
                {lodging.name} confirmó tu pago: tu reserva está saldada. No tenés que hacer nada más.
              </p>
            </div>
          ) : (
            <div className="card grid gap-3 p-5 text-center">
              <p className="font-bold text-ink">Pago pendiente de confirmación</p>
              <p className="text-sm text-muted">
                Mandale el capture del pago por WhatsApp a {lodging.name}. Cuando el anfitrión lo verifique vas a ver
                acá <span className="font-bold text-ink">«Pago aceptado»</span>.
              </p>
              <a
                href={waLink(paymentMessage)}
                target="_blank"
                rel="noreferrer"
                className="mx-auto inline-flex items-center gap-2 rounded-xl bg-lime px-5 py-3 text-sm font-bold text-lime-ink transition hover:opacity-90"
              >
                <WhatsAppIcon size={18} />
                Enviar capture al WhatsApp
              </a>
              <p className="text-xs text-muted">
                WhatsApp de la empresa: <span className="font-semibold text-ink">{lodging.phone}</span>
              </p>
            </div>
          )}

          {/* Lancha solicitada: queda PENDIENTE, la empresa se comunica */}
          {booking.boatQuote?.status === "REQUESTED" && (
            <div className="card grid gap-2 p-5">
              <p className="font-bold text-ink">Tu solicitud de lancha está pendiente</p>
              <p className="text-sm text-muted">
                Quedó registrada como <span className="font-semibold text-ink">pendiente</span>: {lodging.name} se va
                a comunicar con vos para coordinar el horario y el precio del traslado.
              </p>
            </div>
          )}

          {booking.boatQuote?.status === "REQUESTED" && quotePayload && (
            <div className="card p-5">
              <p className="mb-1 font-bold text-ink">Datos de tu solicitud de lancha</p>
              <details className="text-sm">
                <summary className="cursor-pointer text-teal-strong">Ver detalle de la solicitud</summary>
                <pre className="mt-3 overflow-x-auto rounded-xl bg-cloud p-3 text-xs text-ink-soft">
                  {JSON.stringify(quotePayload, null, 2)}
                </pre>
              </details>
              <div className="mt-3">
                <Button type="button" variant="secondary" icon={copied ? Check : Copy} onClick={copyJson} className="min-h-[40px] px-4">
                  {copied ? "Copiado" : "Copiar datos"}
                </Button>
              </div>
            </div>
          )}

          {DEMO_MODE && (
            <Alert tone="info">
              Estás viendo una demostración: estos datos se guardan en tu navegador y no se envían a ningún servidor.
            </Alert>
          )}
        </div>
      </OnboardingShell>
    );
  }

  // ── Portada: presentación del alojamiento con sus fotos ──────────────────
  if (!started) {
    return (
      <OnboardingShell lodging={lodging} wide>
        <OnboardingIntro
          lodging={lodging}
          booking={booking}
          nights={nights}
          photos={stepPhotos}
          stepsCount={STEPS.length}
          waLink={waLink}
          onStart={() => {
            setStarted(true);
            window.scrollTo({ top: 0, behavior: "smooth" });
          }}
        />
      </OnboardingShell>
    );
  }

  // ── Flujo por pasos ──────────────────────────────────────────────────────
  return (
    <OnboardingShell lodging={lodging}>
      <div className="grid gap-6">
        {/* Progreso: etiqueta del paso + barra segmentada con la marca */}
        <div className="grid gap-2">
          <div className="flex items-center justify-between gap-3 text-[11px] font-bold">
            <span className="inline-flex min-w-0 items-center gap-1.5 text-teal-strong">
              {(() => {
                const Icon = STEPS[step].icon;
                return <Icon size={13} aria-hidden="true" />;
              })()}
              <span className="truncate">{STEPS[step].label}</span>
            </span>
            <span className="shrink-0 text-muted" aria-hidden="true">
              Paso {step + 1} de {STEPS.length}
            </span>
          </div>
          <div
            className="flex gap-1.5"
            role="progressbar"
            aria-label="Progreso de la ficha de llegada"
            aria-valuemin={1}
            aria-valuemax={STEPS.length}
            aria-valuenow={step + 1}
            aria-valuetext={`Paso ${step + 1} de ${STEPS.length}: ${STEPS[step].label}`}
          >
            {STEPS.map((item, index) => (
              <span
                key={item.id}
                className="h-2 flex-1 rounded-full transition-colors duration-300"
                style={{
                  backgroundColor:
                    index <= step ? "var(--hf-hero)" : "var(--color-overlay, rgba(17, 17, 17, 0.06))",
                }}
              />
            ))}
          </div>
        </div>

        {/* Paso 0: datos del huésped */}
        {step === 0 && (
          <div className="card grid gap-4 overflow-hidden p-6 animate-rise">
            <StepHeading
              icon={UserRound}
              eyebrow="Ficha de llegada"
              title={`¡Hola, ${booking.guestName.split(" ")[0]}!`}
              text={`Completá tus datos para ${lodging.name}. Solo toma un minuto y no necesitás crear cuenta.`}
              aside={
                <div className="flex flex-wrap items-center gap-1.5">
                  <Badge tone="teal">
                    <Clock size={11} /> 1 minuto
                  </Badge>
                  <Badge tone="lime">
                    <ShieldCheck size={11} /> Sin cuenta
                  </Badge>
                </div>
              }
            />

            {/* Tarjeta de la estadía: fechas, noches y huéspedes con la marca */}
            <StayCard booking={booking} lodging={lodging} nights={nights} />

            <Field
              label={
                <span className="inline-flex items-center gap-1.5">
                  <UserRound size={13} className="text-teal-strong" aria-hidden="true" /> Nombre y apellido
                </span>
              }
              name="guestName"
              autoComplete="name"
              value={guest.guestName}
              onChange={setGuestField("guestName")}
              onBlur={blurGuestField("guestName")}
              error={fieldErrors.guestName}
              aria-invalid={Boolean(fieldErrors.guestName)}
              autoFocus
            />
            <Field
              label={
                <span className="inline-flex items-center gap-1.5">
                  <IdCard size={13} className="text-teal-strong" aria-hidden="true" /> DNI / Pasaporte
                </span>
              }
              name="guestDocument"
              value={guest.guestDocument}
              onChange={setGuestField("guestDocument")}
              onBlur={blurGuestField("guestDocument")}
              error={fieldErrors.guestDocument}
              aria-invalid={Boolean(fieldErrors.guestDocument)}
              placeholder="Ej. 30.123.456 o AB123456"
              hint="Tal como figura en tu documento."
              required
            />
            <Field
              label={
                <span className="inline-flex items-center gap-1.5">
                  <Mail size={13} className="text-teal-strong" aria-hidden="true" /> Correo electrónico
                </span>
              }
              name="guestEmail"
              type="email"
              autoComplete="email"
              value={guest.guestEmail}
              onChange={setGuestField("guestEmail")}
              onBlur={blurGuestField("guestEmail")}
              error={fieldErrors.guestEmail}
              aria-invalid={Boolean(fieldErrors.guestEmail)}
            />
            <Field
              label={
                <span className="inline-flex items-center gap-1.5">
                  <Phone size={13} className="text-teal-strong" aria-hidden="true" /> Teléfono / WhatsApp
                </span>
              }
              name="guestPhone"
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              value={guest.guestPhone}
              onChange={setGuestField("guestPhone")}
              onBlur={blurGuestField("guestPhone")}
              error={fieldErrors.guestPhone}
              aria-invalid={Boolean(fieldErrors.guestPhone)}
              placeholder="Ej. +54 9 11 5555 0101"
              hint="Con código de área. Lo usamos para coordinar tu llegada."
              required
            />

            {error && <Alert>{error}</Alert>}

            <div className="flex items-start gap-2 rounded-xl border border-line bg-cloud px-3.5 py-3 text-xs text-muted">
              <ShieldCheck size={15} className="mt-0.5 shrink-0 text-teal-strong" aria-hidden="true" />
              <span>Tus datos se comparten solo con {lodging.name} para preparar tu llegada.</span>
            </div>
          </div>
        )}

        {/* Paso 1: alergias y preferencias */}
        {step === 1 && (
          <div className="card grid gap-4 overflow-hidden p-6 animate-rise">
            <StepHeading
              icon={HeartPulse}
              eyebrow="Salud y preferencias"
              title="Alérgenos y preferencias"
              text={`Esta información ayuda a ${lodging.name} a recibirte mejor.`}
            />

            <Alert tone="info">
              Solo la ve {lodging.name} y su equipo: no se comparte con otros huéspedes.
            </Alert>

            <Field
              as="textarea"
              label={
                <span className="inline-flex items-center gap-1.5">
                  <HeartPulse size={13} className="text-teal-strong" aria-hidden="true" /> Alergias alimentarias o de
                  otro tipo
                </span>
              }
              name="allergies"
              value={health.allergies}
              onChange={setHealthField("allergies")}
              placeholder="Ej. Mariscos, gluten, penicilina…"
              hint="Dejalo en blanco si no tenés alergias."
            />
            <Field
              as="textarea"
              label={
                <span className="inline-flex items-center gap-1.5">
                  <Sparkles size={13} className="text-teal-strong" aria-hidden="true" /> Preferencias
                </span>
              }
              name="preferences"
              value={health.preferences}
              onChange={setHealthField("preferences")}
              placeholder="Ej. Desayuno temprano, cuna para bebé, piso alto…"
            />
            <Field
              as="textarea"
              label={
                <span className="inline-flex items-center gap-1.5">
                  <MessageSquare size={13} className="text-teal-strong" aria-hidden="true" /> Comentarios para el
                  alojamiento (opcional)
                </span>
              }
              name="notes"
              value={health.notes}
              onChange={setHealthField("notes")}
              placeholder="Ej. Llegamos tarde la primera noche."
            />
          </div>
        )}

        {/* Paso 2: servicios adicionales */}
        {step === 2 && (
          <div className="card grid gap-4 overflow-hidden p-6 animate-rise">
            <StepHeading
              icon={ClipboardList}
              eyebrow="Extras de tu estadía"
              title="Servicios adicionales"
              text="Sumá extras a tu estadía. Se agregan al total del pedido, que pagás por transferencia."
              aside={
                <span className="rounded-full bg-lime-soft px-2.5 py-1 text-[11px] font-extrabold text-lime-ink">
                  {selected.length === 0
                    ? "Opcional"
                    : `${selected.length} ${selected.length === 1 ? "elegido" : "elegidos"}${
                        servicesTotal > 0 ? ` · ${formatMoney(servicesTotal, lodging.currency)}` : ""
                      }`}
                </span>
              }
            />

            {availableServices.length === 0 ? (
              <p className="text-sm text-muted">Este alojamiento no ofrece servicios adicionales por el momento.</p>
            ) : (
              <div className="grid gap-3">
                {availableServices.map((service) => {
                  const item = selected.find((entry) => entry.serviceId === service.id);
                  const active = Boolean(item);
                  const nightly = service.pricing === "PER_NIGHT";
                  const quantity = item?.quantity || 1;
                  return (
                    <div
                      key={service.id}
                      className={cn(
                        "rounded-xl border transition-all",
                        active ? "border-[color:var(--hf-hero)] bg-teal-soft" : "border-line bg-paper hover:border-line-strong",
                      )}
                    >
                      <button
                        type="button"
                        onClick={() => toggleService(service)}
                        aria-pressed={active}
                        className="flex w-full cursor-pointer items-center justify-between gap-3 px-4 py-3.5 text-left"
                      >
                        <span className="min-w-0">
                          <span className="flex flex-wrap items-center gap-1.5">
                            <span className="block text-sm font-bold text-ink">{service.name}</span>
                            <span className="rounded-full bg-overlay px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-ink-soft">
                              {nightly ? "Por noche" : "Precio fijo"}
                            </span>
                            {service.perGuest && (
                              <span className="rounded-full bg-lime-soft px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-lime-ink">
                                Por huésped
                              </span>
                            )}
                          </span>
                          {service.description && <span className="mt-0.5 block text-xs text-muted">{service.description}</span>}
                        </span>
                        <span className="flex shrink-0 items-center gap-2.5">
                          <span className="text-right">
                            <span className="block text-sm font-extrabold text-ink">
                              {service.price === 0 ? "Gratis" : formatMoney(service.price, lodging.currency)}
                            </span>
                            {service.price > 0 && (nightly || service.perGuest) && (
                              <span className="block text-[10px] font-semibold text-muted">
                                {nightly && service.perGuest
                                  ? "por noche y huésped"
                                  : nightly
                                    ? "por noche"
                                    : "por huésped"}
                              </span>
                            )}
                          </span>
                          <span
                            className={cn(
                              "grid size-6 place-items-center rounded-full border-2",
                              active
                                ? "border-[color:var(--hf-hero)] bg-[color:var(--hf-hero)] text-[color:var(--hf-hero-ink)]"
                                : "border-line-strong text-transparent",
                            )}
                          >
                            <Check size={14} />
                          </span>
                        </span>
                      </button>

                      {/* Detalle de la selección: por noche y/o por huésped se
                          calcula solo; el resto elige cantidad. */}
                      {active && (
                        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line px-4 py-3">
                          {nightly || item.perGuest ? (
                            <span className="text-xs font-semibold text-ink-soft">
                              {nightly && <>{plural(nights, "noche", "noches")} × </>}
                              {item.perGuest && <>{plural(guests, "huésped", "huéspedes")} × </>}
                              {formatMoney(service.price, lodging.currency)} ={" "}
                              <span className="font-extrabold text-ink">
                                {formatMoney(serviceLineTotal(item, nights, guests), lodging.currency)}
                              </span>
                            </span>
                          ) : (
                            <>
                              <span className="text-xs font-semibold text-ink-soft">Cantidad</span>
                              <div className="flex items-center gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => setQuantity(service.id, -1)}
                                  disabled={quantity <= 1}
                                  aria-label={`Quitar una unidad de ${service.name}`}
                                  className="grid size-9 cursor-pointer place-items-center rounded-lg border border-line-strong bg-paper text-base font-bold text-ink transition hover:border-ink/40 disabled:cursor-not-allowed disabled:opacity-40"
                                >
                                  −
                                </button>
                                <span className="w-7 text-center text-sm font-extrabold text-ink" aria-live="polite" aria-label={`Cantidad de ${service.name}`}>
                                  {quantity}
                                </span>
                                <button
                                  type="button"
                                  onClick={() => setQuantity(service.id, 1)}
                                  disabled={quantity >= MAX_SERVICE_QTY}
                                  aria-label={`Agregar una unidad de ${service.name}`}
                                  className="grid size-9 cursor-pointer place-items-center rounded-lg border border-line-strong bg-paper text-base font-bold text-ink transition hover:border-ink/40 disabled:cursor-not-allowed disabled:opacity-40"
                                >
                                  +
                                </button>
                                <span className="ml-1.5 text-xs font-extrabold text-ink">
                                  {formatMoney(serviceLineTotal(item, nights, guests), lodging.currency)}
                                </span>
                              </div>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Paso 3: cotización de lancha (opcional) */}
        {step === 3 && (
          <div className="card overflow-hidden p-6 animate-rise">
            <div className="mt-4">
              <BoatQuoteStep quote={quote} onChange={setQuote} booking={booking} lodging={lodging} />
            </div>
          </div>
        )}

        {/* Paso 4: resumen y confirmación */}
        {step === 4 && (
          <div className="card grid gap-4 p-6 animate-rise">
            <StepHeading
              icon={Check}
              eyebrow="Último paso"
              title="Revisá tu información"
              text={`Al confirmar, ${lodging.name} recibe tu ficha de llegada.`}
              aside={
                <span className="rounded-full bg-lime-soft px-2.5 py-1 text-[11px] font-extrabold text-lime-ink">
                  Total {formatMoney(orderTotal, booking.currency)}
                </span>
              }
            />

            <div className="grid gap-3 break-words rounded-2xl bg-cloud p-4 text-sm">
              <p>
                <span className="font-semibold text-ink">Huésped:</span> {guest.guestName} · {guest.guestEmail}
                {guest.guestPhone && ` · ${guest.guestPhone}`}
              </p>
              <p>
                <span className="font-semibold text-ink">Estadía:</span> {formatDate(booking.checkIn)} →{" "}
                {formatDate(booking.checkOut)} ({plural(nights, "noche", "noches")})
              </p>
              {health.allergies && (
                <p>
                  <span className="font-semibold text-ink">Alergias:</span> {health.allergies}
                </p>
              )}
              {health.preferences && (
                <p>
                  <span className="font-semibold text-ink">Preferencias:</span> {health.preferences}
                </p>
              )}
              <p>
                <span className="font-semibold text-ink">Traslado en lancha:</span>{" "}
                {quote.decided && quote.wanted
                  ? `Sí, cotizar (${quote.pickupPlace || "lugar a confirmar"}${
                      quote.pickupDate ? ` · ${formatDate(quote.pickupDate)}` : ""
                    }${quote.pickupTime ? ` ${quote.pickupTime}` : ""})`
                  : "No solicitado"}
              </p>
            </div>

            {/* Resumen del pedido + datos bancarios (información de pago) */}
            <div className="grid gap-3 rounded-2xl border border-line p-4 text-sm">
              <p className="font-bold text-ink">Resumen del pedido</p>
              {/* min-w-0 en las filas: como grid items su min-width auto empuja el layout en pantallas angostas */}
              <div className="flex min-w-0 items-center justify-between gap-3">
                <span className="min-w-0 truncate text-muted">Alojamiento · {plural(nights, "noche", "noches")}</span>
                <span className="shrink-0 font-semibold text-ink">{formatMoney(booking.total, booking.currency)}</span>
              </div>
              {selected.map((item) => (
                <div key={item.serviceId} className="flex min-w-0 items-center justify-between gap-3">
                  <span className="min-w-0 truncate text-muted">
                    {item.name}
                    {serviceLineDetail(item, nights, guests) && (
                      <span className="ml-1.5 text-[11px] font-semibold text-muted">{serviceLineDetail(item, nights, guests)}</span>
                    )}
                  </span>
                  <span className="shrink-0 font-semibold text-ink">
                    {formatMoney(serviceLineTotal(item, nights, guests), booking.currency)}
                  </span>
                </div>
              ))}
              <div className="mt-1 flex min-w-0 items-center justify-between gap-3 border-t border-line pt-2.5">
                <span className="font-bold text-ink">Total</span>
                <span className="shrink-0 text-base font-extrabold text-ink">{formatMoney(orderTotal, booking.currency)}</span>
              </div>

              {/* Métodos de pago disponibles con opción de copiar credenciales */}
              <div className="mt-3 grid gap-3 rounded-2xl bg-cloud/70 p-4">
                <div className="flex flex-wrap items-center justify-between gap-1.5">
                  <p className="font-bold text-ink">Métodos de pago habilitados</p>
                  <span className="text-[11px] font-semibold text-muted">Copiá los datos para abonar</span>
                </div>

                {activePaymentMethods.length > 0 ? (
                  <div className="grid gap-2.5">
                    {activePaymentMethods.map((method) => {
                      const keys = method.keys || {};
                      const isCopied = copiedMethodId === method.id;
                      return (
                        <div
                          key={method.id}
                          className="flex flex-col justify-between gap-3 rounded-xl border border-line bg-paper p-3.5 shadow-sm sm:flex-row sm:items-center"
                        >
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="grid size-7 place-items-center rounded-lg bg-teal-soft text-teal-strong">
                                {method.type === "BANK" ? <Landmark size={14} /> : <CreditCard size={14} />}
                              </span>
                              <span className="text-sm font-bold text-ink">{method.label}</span>
                            </div>

                            <div className="mt-1.5 grid gap-0.5 text-xs text-muted">
                              {keys.holder && (
                                <p>
                                  <span className="font-semibold text-ink">Titular:</span> {keys.holder}
                                </p>
                              )}
                              {keys.alias && (
                                <p>
                                  <span className="font-semibold text-ink">Alias:</span>{" "}
                                  <code className="font-mono text-teal-strong">{keys.alias}</code>
                                </p>
                              )}
                              {keys.cbu && (
                                <p>
                                  <span className="font-semibold text-ink">CBU:</span>{" "}
                                  <code className="font-mono text-teal-strong">{keys.cbu}</code>
                                </p>
                              )}
                              {keys.cvu && (
                                <p>
                                  <span className="font-semibold text-ink">CVU:</span>{" "}
                                  <code className="font-mono text-teal-strong">{keys.cvu}</code>
                                </p>
                              )}
                              {keys.email && (
                                <p>
                                  <span className="font-semibold text-ink">Correo:</span> {keys.email}
                                </p>
                              )}
                              {keys.paypalMe && (
                                <p>
                                  <span className="font-semibold text-ink">PayPal.Me:</span> {keys.paypalMe}
                                </p>
                              )}
                              {keys.publicKey && (
                                <p>
                                  <span className="font-semibold text-ink">Llave pública:</span> {keys.publicKey}
                                </p>
                              )}
                              {method.notes && <p className="italic text-muted">{method.notes}</p>}
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() => copyMethodCredentials(method)}
                            className={cn(
                              "inline-flex shrink-0 cursor-pointer items-center justify-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-bold transition-all sm:self-center",
                              isCopied
                                ? "border-success bg-success-soft text-success"
                                : "border-line-strong bg-cloud text-ink hover:border-teal hover:text-teal-strong",
                            )}
                          >
                            {isCopied ? (
                              <>
                                <Check size={14} /> ¡Copiado!
                              </>
                            ) : (
                              <>
                                <Copy size={14} /> Copiar credenciales
                              </>
                            )}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                ) : hasBankDetails ? (
                  <div className="rounded-xl border border-line bg-paper p-3 text-xs text-muted">
                    {bankDetails.holder && <p><span className="font-semibold text-ink">Titular:</span> {bankDetails.holder}</p>}
                    {bankDetails.alias && <p><span className="font-semibold text-ink">Alias:</span> {bankDetails.alias}</p>}
                    {bankDetails.cbu && <p><span className="font-semibold text-ink">CBU:</span> {bankDetails.cbu}</p>}
                    {bankDetails.notes && <p>{bankDetails.notes}</p>}
                  </div>
                ) : (
                  <p className="text-xs text-muted">
                    {lodging.name} te compartirá los datos para realizar el pago al confirmar.
                  </p>
                )}
              </div>
            </div>

            {error && <Alert>{error}</Alert>}

            {!DEMO_MODE && (
              <Alert tone="info">
                El backend aún no expone el módulo de huéspedes: tu ficha queda registrada en este navegador y el
                anfitrión la verá al abrir el enlace desde el mismo equipo.
              </Alert>
            )}
          </div>
        )}

        {/* Navegación entre pasos */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Button type="button" variant="ghost" icon={ArrowLeft} onClick={back} disabled={submitting}>
            Atrás
          </Button>
          {step < STEPS.length - 1 ? (
            <Button type="button" icon={ArrowRight} onClick={next} className="w-full sm:w-auto">
              Continuar
            </Button>
          ) : (
            <Button type="button" icon={Check} onClick={confirm} disabled={submitting || completed} className="w-full sm:w-auto">
              {submitting ? "Enviando…" : "Confirmar llegada"}
            </Button>
          )}
        </div>

        {waDigits && (
          <p className="text-center text-xs text-muted">
            ¿Tenés una duda?{" "}
            <a
              href={waLink(`Hola ${lodging.name}, tengo una consulta sobre mi llegada.`)}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 font-semibold text-teal-strong transition-colors hover:text-teal"
            >
              <WhatsAppIcon size={14} /> Escribinos por WhatsApp
            </a>
          </p>
        )}
      </div>
    </OnboardingShell>
  );
}

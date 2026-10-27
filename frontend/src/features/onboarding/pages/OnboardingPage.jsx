import { useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  ClipboardList,
  Copy,
  HeartPulse,
  MessageCircle,
  Ship,
  UserRound,
} from "lucide-react";
import { Alert, Badge, Button, EmptyState, Field } from "@components/ui";
import OnboardingShell from "../components/OnboardingShell";
import BoatQuoteStep from "../components/BoatQuoteStep";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { isOnboardingLinkExpired } from "@lib/state/store";
import { DEMO_MODE } from "@lib/config/env";
import { bookingGrandTotal, daysBetween, formatDate, formatMoney, isEmail, plural } from "@utils/format";

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

// Onboarding B2C: el huésped accede con el enlace único de su reserva
// (/llegada/:lodgingId/:bookingId), sin crear cuenta. La identidad visual es la
// del alojamiento (OnboardingShell + LodgingTheme).
export default function OnboardingPage() {
  const { lodgingId, bookingId } = useParams();
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const booking = useStore((s) => s.bookings.find((b) => b.id === bookingId));
  const services = useStore((s) => s.services);
  const bankDetails = useStore((s) => s.payments.bankDetails);

  const [step, setStep] = useState(0);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [copied, setCopied] = useState(false);

  const [guest, setGuest] = useState({
    guestName: booking?.guestName || "",
    guestDocument: "",
    guestEmail: booking?.guestEmail || "",
    guestPhone: "",
  });
  const [health, setHealth] = useState({ allergies: "", preferences: "", notes: "" });
  const [selected, setSelected] = useState([]); // [{ serviceId, name, price }]
  const [quote, setQuote] = useState(EMPTY_QUOTE);

  const linkValid = lodgingId === lodging.id;
  const availableServices = useMemo(() => services.filter((sv) => sv.enabled), [services]);
  const nights = booking ? daysBetween(booking.checkIn, booking.checkOut) : 0;
  // Total REAL: alojamiento + servicios adicionales elegidos en el onboarding.
  const servicesTotal = selected.reduce((sum, item) => sum + (Number(item.price) || 0), 0);
  const orderTotal = (Number(booking?.total) || 0) + servicesTotal;
  const savedTotal = booking ? bookingGrandTotal(booking) : orderTotal;
  const hasBankDetails = Boolean(bankDetails.holder || bankDetails.alias || bankDetails.cbu);

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

  const setGuestField = (key) => (event) => setGuest((g) => ({ ...g, [key]: event.target.value }));
  const setHealthField = (key) => (event) => setHealth((h) => ({ ...h, [key]: event.target.value }));

  const toggleService = (service) => {
    setSelected((current) =>
      current.some((item) => item.serviceId === service.id)
        ? current.filter((item) => item.serviceId !== service.id)
        : [...current, { serviceId: service.id, name: service.name, price: service.price }],
    );
  };

  const next = () => {
    setError("");
    if (step === 0) {
      if (!guest.guestName.trim()) return setError("Ingresá tu nombre y apellido.");
      const doc = guest.guestDocument.trim();
      if (!doc) return setError("Ingresá tu DNI o pasaporte.");
      if (!/^[0-9A-Za-z.\- ]{5,20}$/.test(doc)) {
        return setError("El DNI/Pasaporte debe tener entre 5 y 20 caracteres (letras, números, puntos o guiones).");
      }
      if (!guest.guestEmail || !isEmail(guest.guestEmail)) return setError("Ingresá un correo válido.");
      if (guest.guestPhone && !/^[+0-9()\s-]{6,20}$/.test(guest.guestPhone.trim())) {
        return setError("Ingresá un teléfono válido o dejalo vacío.");
      }
    }
    setStep((current) => Math.min(current + 1, STEPS.length - 1));
  };

  const back = () => {
    setError("");
    setStep((current) => Math.max(current - 1, 0));
  };

  // Confirmación final: genera la solicitud de lancha (si la hubo) y guarda el
  // onboarding. Protegido contra dobles envíos (submitting / done).
  const confirm = () => {
    if (submitting || completed) return;
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
              <MessageCircle size={16} aria-hidden="true" />
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
          <div className="card grid gap-3 p-6 text-center">
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
                <MessageCircle size={18} aria-hidden="true" />
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

  // ── Flujo por pasos ──────────────────────────────────────────────────────
  return (
    <OnboardingShell lodging={lodging}>
      <div className="grid gap-6">
        {/* Progreso */}
        <div>
          <div className="mb-2 flex items-center justify-between text-xs font-bold text-muted">
            <span aria-hidden="true">
              Paso {step + 1} de {STEPS.length}
            </span>
            <span>{STEPS[step].label}</span>
          </div>
          <div
            className="h-2 overflow-hidden rounded-full bg-overlay"
            role="progressbar"
            aria-label="Progreso de la ficha de llegada"
            aria-valuemin={1}
            aria-valuemax={STEPS.length}
            aria-valuenow={step + 1}
            aria-valuetext={`Paso ${step + 1} de ${STEPS.length}: ${STEPS[step].label}`}
          >
            <div
              className="h-full rounded-full bg-teal transition-all duration-300"
              style={{ width: `${((step + 1) / STEPS.length) * 100}%` }}
            />
          </div>
        </div>

        {/* Paso 0: datos del huésped */}
        {step === 0 && (
          <div className="card grid gap-4 p-6 animate-rise">
            <div>
              <h1 className="text-xl font-extrabold text-ink">¡Hola, {booking.guestName.split(" ")[0]}!</h1>
              <p className="mt-1.5 text-sm text-muted">
                Completá tu ficha de llegada para {lodging.name}. Solo toma un minuto y no necesitás crear cuenta.
              </p>
            </div>

            <div className="grid gap-3 rounded-2xl bg-cloud p-4 text-sm">
              <span className="font-semibold text-ink">
                {formatDate(booking.checkIn)} → {formatDate(booking.checkOut)} · {plural(nights, "noche", "noches")}
              </span>
              <span className="text-muted">{plural(booking.guests, "huésped", "huéspedes")}</span>
            </div>

            <Field label="Nombre y apellido" name="guestName" value={guest.guestName} onChange={setGuestField("guestName")} autoFocus />
            <Field
              label="DNI / Pasaporte"
              name="guestDocument"
              value={guest.guestDocument}
              onChange={setGuestField("guestDocument")}
              placeholder="Ej. 30.123.456 o AB123456"
              required
            />
            <Field label="Correo electrónico" name="guestEmail" type="email" value={guest.guestEmail} onChange={setGuestField("guestEmail")} />
            <Field
              label="Teléfono / WhatsApp"
              name="guestPhone"
              type="tel"
              value={guest.guestPhone}
              onChange={setGuestField("guestPhone")}
              placeholder="Ej. +54 9 11 5555 0101"
            />

            {error && <Alert>{error}</Alert>}
          </div>
        )}

        {/* Paso 1: alergias y preferencias */}
        {step === 1 && (
          <div className="card grid gap-4 p-6 animate-rise">
            <div>
              <h1 className="text-xl font-extrabold text-ink">Alérgenos y preferencias</h1>
              <p className="mt-1.5 text-sm text-muted">
                Esta información ayuda a {lodging.name} a recibirte mejor.
              </p>
            </div>

            <Field
              as="textarea"
              label="Alergias alimentarias o de otro tipo"
              name="allergies"
              value={health.allergies}
              onChange={setHealthField("allergies")}
              placeholder="Ej. Mariscos, gluten, penicilina…"
              hint="Dejalo en blanco si no tenés alergias."
            />
            <Field
              as="textarea"
              label="Preferencias"
              name="preferences"
              value={health.preferences}
              onChange={setHealthField("preferences")}
              placeholder="Ej. Desayuno temprano, cuna para bebé, piso alto…"
            />
            <Field
              as="textarea"
              label="Comentarios para el alojamiento (opcional)"
              name="notes"
              value={health.notes}
              onChange={setHealthField("notes")}
              placeholder="Ej. Llegamos tarde la primera noche."
            />
          </div>
        )}

        {/* Paso 2: servicios adicionales */}
        {step === 2 && (
          <div className="card grid gap-4 p-6 animate-rise">
            <div>
              <h1 className="text-xl font-extrabold text-ink">Servicios adicionales</h1>
              <p className="mt-1.5 text-sm text-muted">
                Sumá extras a tu estadía. Se agregan al total del pedido, que pagás por transferencia.
              </p>
            </div>

            {availableServices.length === 0 ? (
              <p className="text-sm text-muted">Este alojamiento no ofrece servicios adicionales por el momento.</p>
            ) : (
              <div className="grid gap-3">
                {availableServices.map((service) => {
                  const active = selected.some((item) => item.serviceId === service.id);
                  return (
                    <button
                      key={service.id}
                      type="button"
                      onClick={() => toggleService(service)}
                      aria-pressed={active}
                      className={`flex cursor-pointer items-center justify-between gap-3 rounded-xl border px-4 py-3.5 text-left transition-all ${
                        active ? "border-teal bg-teal-soft" : "border-line bg-paper hover:border-line-strong"
                      }`}
                    >
                      <span className="min-w-0">
                        <span className="block text-sm font-bold text-ink">{service.name}</span>
                        {service.description && <span className="block text-xs text-muted">{service.description}</span>}
                      </span>
                      <span className="flex shrink-0 items-center gap-2.5">
                        <span className="text-sm font-extrabold text-ink">
                          {service.price === 0 ? "Gratis" : formatMoney(service.price, lodging.currency)}
                        </span>
                        <span
                          className={`grid size-6 place-items-center rounded-full border-2 ${
                            active ? "border-teal bg-teal text-white" : "border-line-strong text-transparent"
                          }`}
                        >
                          <Check size={14} />
                        </span>
                      </span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Paso 3: cotización de lancha (opcional) */}
        {step === 3 && (
          <div className="card p-6 animate-rise">
            <BoatQuoteStep quote={quote} onChange={setQuote} booking={booking} lodging={lodging} />
          </div>
        )}

        {/* Paso 4: resumen y confirmación */}
        {step === 4 && (
          <div className="card grid gap-4 p-6 animate-rise">
            <div>
              <h1 className="text-xl font-extrabold text-ink">Revisá tu información</h1>
              <p className="mt-1.5 text-sm text-muted">Al confirmar, {lodging.name} recibe tu ficha de llegada.</p>
            </div>

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
                  <span className="min-w-0 truncate text-muted">{item.name}</span>
                  <span className="shrink-0 font-semibold text-ink">{formatMoney(item.price, booking.currency)}</span>
                </div>
              ))}
              <div className="mt-1 flex min-w-0 items-center justify-between gap-3 border-t border-line pt-2.5">
                <span className="font-bold text-ink">Total</span>
                <span className="shrink-0 text-base font-extrabold text-ink">{formatMoney(orderTotal, booking.currency)}</span>
              </div>

              {selected.length > 0 && (
                <div className="mt-1 rounded-xl bg-cloud p-3.5">
                  <p className="font-semibold text-ink">Para pagar por transferencia</p>
                  {hasBankDetails ? (
                    <div className="mt-1.5 grid gap-1 text-xs text-muted">
                      {bankDetails.holder && <p><span className="font-semibold text-ink">Titular:</span> {bankDetails.holder}</p>}
                      {bankDetails.alias && <p><span className="font-semibold text-ink">Alias:</span> {bankDetails.alias}</p>}
                      {bankDetails.cbu && <p><span className="font-semibold text-ink">CBU:</span> {bankDetails.cbu}</p>}
                      {bankDetails.notes && <p>{bankDetails.notes}</p>}
                    </div>
                  ) : (
                    <p className="mt-1 text-xs text-muted">
                      {lodging.name} te compartirá los datos de la transferencia para confirmar el pedido.
                    </p>
                  )}
                </div>
              )}
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
          <Button type="button" variant="ghost" icon={ArrowLeft} onClick={back} disabled={step === 0 || submitting}>
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
      </div>
    </OnboardingShell>
  );
}

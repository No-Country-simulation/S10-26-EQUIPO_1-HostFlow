import { useEffect, useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router";
import { ArrowLeft, ArrowRight, Check, ClipboardList } from "lucide-react";
import { Button, EmptyState, WhatsAppIcon } from "@components/ui";
import { useBooking, useLodging, usePayments, useServices } from "@lib/api/hooks";
import { useSession } from "@features/auth";
import { isOnboardingLinkExpired } from "@lib/domain/onboarding";
import { DEMO_MODE } from "@lib/config/env";
import { bookingGrandTotal, copyText, daysBetween, formatMoney, serviceLineTotal } from "@utils/format";
import OnboardingShell from "../components/OnboardingShell";
import OnboardingIntro from "../components/OnboardingIntro";
import BoatQuoteStep from "../components/BoatQuoteStep";
import OnboardingDone from "../components/OnboardingDone";
import OnboardingProgress from "../components/OnboardingProgress";
import GuestStep from "../components/steps/GuestStep";
import HealthStep from "../components/steps/HealthStep";
import ServicesStep from "../components/steps/ServicesStep";
import ConfirmStep from "../components/steps/ConfirmStep";
import {
  BOAT_FIELDS,
  EMPTY_QUOTE,
  GUEST_FIELDS,
  MAX_SERVICE_QTY,
  OTHER_ALLERGY,
  cleanGuest,
  splitName,
  validateBoat,
  validateGuest,
} from "../lib/onboardingForm";
import { STEPS } from "../lib/steps";

// Onboarding B2C: el huésped accede con el enlace único de su reserva
// (/llegada/:lodgingId/:bookingId), sin crear cuenta. La identidad visual es la
// del alojamiento (OnboardingShell + LodgingTheme).
export default function OnboardingPage() {
  const { lodgingId, bookingId } = useParams();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const effectiveBookingId = token || bookingId;
  const { actions } = useSession();
  const lodging = useLodging();
  const booking = useBooking(effectiveBookingId);
  const services = useServices();
  const payments = usePayments();
  const bankDetails = payments?.bankDetails || {};
  const paymentMethods = payments?.methods || [];

  const [step, setStep] = useState(0);
  const [started, setStarted] = useState(false);
  const [error, setError] = useState("");
  const [loadingLink, setLoadingLink] = useState(Boolean(token && !DEMO_MODE));
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [copied, setCopied] = useState(false);
  const [copiedMethodId, setCopiedMethodId] = useState(null);

  const [guest, setGuest] = useState(() => {
    const { firstName, lastName } = splitName(booking?.guestName);
    return {
      firstName,
      lastName,
      guestDocument: "",
      guestEmail: booking?.guestEmail || "",
      guestPhone: booking?.guestPhone || "",
    };
  });
  // Alergias: multiselección (array) + "Otro" con detalle libre.
  const [health, setHealth] = useState({ preferences: "" });
  const [allergies, setAllergies] = useState([]);
  const [allergyOther, setAllergyOther] = useState("");
  const [selected, setSelected] = useState([]); // [{ serviceId, name, price }]
  const [quote, setQuote] = useState(EMPTY_QUOTE);
  const [boatErrors, setBoatErrors] = useState({});
  // Método de pago elegido en el resumen: se muestra solo ese, no todos.
  const [paymentMethodId, setPaymentMethodId] = useState(null);

  // El huésped abre el enlace desde su propio dispositivo, donde no hay datos
  // locales: se piden al backend (endpoint público, sin token) el alojamiento,
  // la reserva, los servicios y las formas de pago con sus credenciales.
  // En modo demo todo ya vive en el store del navegador.
  useEffect(() => {
    if (DEMO_MODE || (!token && (!lodgingId || !bookingId))) return;
    let alive = true;
    setLoadingLink(true);
    Promise.resolve()
      .then(() => token ? actions.getOnboarding?.(token) : actions.getOnboarding?.(lodgingId, bookingId))
      .catch((err) => {
        if (alive) setError(err?.message || "No se pudo cargar el enlace de llegada.");
      })
      .finally(() => {
        if (alive) setLoadingLink(false);
      });
    return () => {
      alive = false;
    };
  }, [actions, lodgingId, bookingId, token]);

  const linkValid = token ? true : lodgingId === lodging.id;
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

  // Alergias finales: las elegidas + lo escrito en "Otro" (puede traer varias
  // separadas por coma). Siempre se envía como array.
  const allergyList = useMemo(() => {
    const other = allergies.includes(OTHER_ALLERGY)
      ? allergyOther.split(",").map((item) => item.trim()).filter(Boolean)
      : [];
    return [...allergies.filter((item) => item !== OTHER_ALLERGY), ...other];
  }, [allergies, allergyOther]);

  // Método elegido en el resumen (por defecto, el primero habilitado).
  const chosenMethod = activePaymentMethods.find((m) => m.id === paymentMethodId) || activePaymentMethods[0] || null;
  // Las billeteras digitales se pagan en dólares/euros: mostramos la conversión
  // de cada precio en las monedas secundarias que habilitó el alojamiento.
  const walletCodes = useMemo(() => {
    const codes = Array.isArray(payments?.wallets) ? payments.wallets : [];
    const valid = codes.filter((code) => code !== lodging.currency);
    return valid.length ? valid : ["USD", "EUR"];
  }, [payments?.wallets, lodging.currency]);
  const showConversion = chosenMethod?.type === "WALLET";

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
      // Normaliza espacios sin perder los dos campos.
      setGuest((current) => ({
        ...current,
        firstName: current.firstName.trim().replace(/\s+/g, " "),
        lastName: current.lastName.trim().replace(/\s+/g, " "),
        guestDocument: current.guestDocument.trim().replace(/\s+/g, " "),
        guestEmail: current.guestEmail.trim(),
        guestPhone: current.guestPhone.trim(),
      }));
    }
    if (step === 1 && allergies.includes(OTHER_ALLERGY) && !allergyOther.trim()) {
      setError("Especificá cuál es la alergia en «Otro» o quitá esa opción.");
      document.querySelector('[name="allergyOther"]')?.focus();
      return;
    }
    if (step === 3) {
      // "Continuar" pide la cotización: los datos del viaje son obligatorios.
      // Si algo falta o está mal, no se avanza. "Omitir" es el único atajo.
      const boat = validateBoat(quote, booking);
      setBoatErrors(boat);
      const firstInvalid = BOAT_FIELDS.find((key) => boat[key]);
      if (firstInvalid) {
        document.querySelector(`[name="${firstInvalid}"]`)?.focus();
        return;
      }
      // Datos completos: queda registrada la intención de cotizar.
      setQuote((current) => ({ ...current, decided: true, wanted: true }));
    }
    setStep((current) => Math.min(current + 1, STEPS.length - 1));
  };

  // Omitir el traslado: no se pide cotización y se sigue con la llegada.
  const omitBoat = () => {
    setBoatErrors({});
    setError("");
    setQuote((current) => ({ ...current, decided: true, wanted: false }));
    setStep((current) => Math.min(current + 1, STEPS.length - 1));
  };

  // Cambios en el formulario: se limpia el error del campo que se corrige.
  const updateQuote = (nextQuote) => {
    setQuote(nextQuote);
    setBoatErrors((current) => {
      const changed = Object.keys(nextQuote).filter((key) => nextQuote[key] !== quote[key]);
      if (!changed.some((key) => current[key])) return current;
      const copy = { ...current };
      changed.forEach((key) => delete copy[key]);
      return copy;
    });
  };

  const back = () => {
    setError("");
    setBoatErrors({});
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
      const data = cleanGuest(guest);
      if (quote.wanted) {
        await actions.createBoatQuoteRequest(booking.id, {
          guestName: data.guestName,
          guestEmail: data.guestEmail,
          guestPhone: data.guestPhone,
          pickupPlace: quote.pickupPlace,
          pickupDate: quote.pickupDate,
          pickupTime: quote.pickupTime,
          passengers: quote.passengers,
          baggage: quote.baggage,
          dropoffPlace: quote.dropoffPlace || lodging.location,
          notes: quote.notes,
        });
      } else if (quote.decided) {
        await actions.declineBoatQuote(booking.id);
      }
      await actions.submitOnboarding(booking.id, {
        ...data,
        ...health,
        allergies: allergyList,
        selectedServices: selected,
        boatQuoteRequested: Boolean(quote.wanted),
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
          title={loadingLink ? "Cargando tu reserva…" : "No encontramos tu reserva"}
          text={loadingLink ? "Estamos verificando tu enlace de llegada." : error || "Este enlace de llegada ya no está disponible. Consultá con tu anfitrión."}
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
    return (
      <OnboardingShell lodging={lodging}>
        <OnboardingDone booking={booking} lodging={lodging} paid={paid} copyJson={copyJson} copied={copied} />
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
        <OnboardingProgress steps={STEPS} step={step} />

        {step === 0 && (
          <GuestStep
            booking={booking}
            guest={guest}
            fieldErrors={fieldErrors}
            error={error}
            lodging={lodging}
            nights={nights}
            setGuestField={setGuestField}
            blurGuestField={blurGuestField}
          />
        )}

        {step === 1 && (
          <HealthStep
            allergies={allergies}
            setAllergies={setAllergies}
            allergyOther={allergyOther}
            setAllergyOther={setAllergyOther}
            health={health}
            setHealthField={setHealthField}
            error={error}
            lodging={lodging}
          />
        )}

        {step === 2 && (
          <ServicesStep
            availableServices={availableServices}
            selected={selected}
            toggleService={toggleService}
            setQuantity={setQuantity}
            nights={nights}
            guests={guests}
            servicesTotal={servicesTotal}
            lodging={lodging}
          />
        )}

        {/* Paso 3: cotización de lancha remis (opcional) */}
        {step === 3 && (
          <div className="card grid gap-4 overflow-hidden p-5 animate-rise sm:p-6">
            <BoatQuoteStep quote={quote} onChange={updateQuote} booking={booking} lodging={lodging} errors={boatErrors} />
          </div>
        )}

        {step === 4 && (
          <ConfirmStep
            booking={booking}
            guest={guest}
            health={health}
            allergyList={allergyList}
            quote={quote}
            lodging={lodging}
            nights={nights}
            guests={guests}
            orderTotal={orderTotal}
            selected={selected}
            showConversion={showConversion}
            walletCodes={walletCodes}
            waLink={waLink}
            paymentMessage={paymentMessage}
            waDigits={waDigits}
            activePaymentMethods={activePaymentMethods}
            chosenMethod={chosenMethod}
            paymentMethodId={paymentMethodId}
            setPaymentMethodId={setPaymentMethodId}
            copyMethodCredentials={copyMethodCredentials}
            copiedMethodId={copiedMethodId}
            hasBankDetails={hasBankDetails}
            bankDetails={bankDetails}
            error={error}
          />
        )}

        {/* Navegación entre pasos: primario a la derecha (en móvil, ancho completo) */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Button type="button" variant="ghost" icon={ArrowLeft} onClick={back} disabled={submitting}>
            Atrás
          </Button>
          <div className="flex w-full flex-wrap items-center justify-end gap-2 sm:w-auto">
            {step === 3 && (
              <Button type="button" variant="secondary" onClick={omitBoat} disabled={submitting} className="flex-1 sm:flex-none">
                Omitir
              </Button>
            )}
            {step < STEPS.length - 1 ? (
              <Button type="button" icon={ArrowRight} onClick={next} className="flex-1 sm:flex-none">
                Continuar
              </Button>
            ) : (
              <Button type="button" icon={Check} onClick={confirm} disabled={submitting || completed} className="flex-1 sm:flex-none">
                {submitting ? "Enviando…" : "Confirmar llegada"}
              </Button>
            )}
          </div>
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

import { useMemo, useRef, useState } from "react";
import { Alert, Button, Field, Modal } from "@components/ui";
import { BookingRequirementsAlert } from "./BookingRequirementsAlert";
import { getBookingRequirements } from "../lib/bookingRequirements";
import { useBlockedDays, useBookings, useLodging, usePayments } from "@lib/api/hooks";
import { useSession } from "@features/auth";
import { daysBetween, formatMoney, isEmail, plural, toISODate } from "@utils/format";

// Teléfono del huésped: obligatorio, igual que en el onboarding, porque es el
// canal para coordinar la llegada (y queda precargado en su ficha).
function isPhone(value) {
  const digits = value.replace(/\D/g, "");
  return /^[+0-9()\s.-]+$/.test(value) && digits.length >= 6 && digits.length <= 15;
}

// Validación campo por campo: cada mensaje se muestra DEBAJO de su input (igual
// que en el onboarding), no en un cartel suelto al final del formulario.
const FIELD_ORDER = ["guestName", "guestEmail", "guestPhone", "checkIn", "checkOut", "guests"];

const readForm = (form) => ({
  guestName: String(form.get("guestName") || "").trim(),
  guestEmail: String(form.get("guestEmail") || "").trim(),
  guestPhone: String(form.get("guestPhone") || "").trim(),
  checkIn: String(form.get("checkIn") || ""),
  checkOut: String(form.get("checkOut") || ""),
  guests: Number(form.get("guests")),
});

const validateFields = (data) => ({
  guestName: !data.guestName ? "Ingresá el nombre del huésped." : "",
  guestEmail: !data.guestEmail
    ? "Ingresá el correo del huésped."
    : !isEmail(data.guestEmail)
      ? "Ingresá un correo válido."
      : "",
  guestPhone: !data.guestPhone
    ? "Ingresá el teléfono del huésped."
    : !isPhone(data.guestPhone)
      ? "Revisá el teléfono del huésped: tiene que ser un número real."
      : "",
  checkIn: !data.checkIn ? "Elegí la fecha de ingreso." : "",
  checkOut: !data.checkOut
    ? "Elegí la fecha de salida."
    : data.checkOut <= data.checkIn
      ? "La salida debe ser posterior al ingreso."
      : "",
  guests: !data.guests || data.guests < 1 ? "La cantidad de huéspedes debe ser al menos 1." : "",
});

// Alta de una reserva desde el panel del anfitrión (entregable C).
// Requisitos previos (spec): antes de crear una reserva el alojamiento debe
// tener al menos 1 método de pago, el logo subido, la descripción de
// habitaciones/baños/capacidad y disponibilidad en las fechas elegidas.
export function BookingFormModal({ open, onClose, onCreated }) {
  const { actions } = useSession();
  const lodging = useLodging();
  const payments = usePayments();
  const bookings = useBookings();
  const blockedDays = useBlockedDays();
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [saving, setSaving] = useState(false);
  // Fechas elegidas en el form: alimentan el chequeo de disponibilidad en vivo.
  const [dates, setDates] = useState({ checkIn: "", checkOut: "" });

  const today = toISODate();

  // Configuración mínima del alojamiento (cada alerta lleva a la pantalla que
  // la completa). Si falta algo, la reserva no se crea y se muestra el enlace.
  const requirements = useMemo(() => getBookingRequirements(lodging, payments), [lodging, payments]);
  // Referencia a la alerta: si se intenta crear sin los datos, se lleva el foco
  // visual ahí para que el anfitrión vea los accesos.
  const requirementsRef = useRef(null);

  // Disponibilidad: solape con otras reservas o días bloqueados en [checkIn, checkOut).
  const findConflict = (checkIn, checkOut) => {
    if (!checkIn || !checkOut || checkOut <= checkIn) return "";
    if (blockedDays.some((d) => d >= checkIn && d < checkOut)) {
      return "Hay días bloqueados en esas fechas: revisá la pantalla de Disponibilidad.";
    }
    const clash = bookings.find(
      (b) => b.status !== "CANCELLED" && b.checkIn < checkOut && b.checkOut > checkIn,
    );
    if (clash) return `Esas fechas ya tienen una reserva de ${clash.guestName}.`;
    return "";
  };

  const conflict = useMemo(
    () => findConflict(dates.checkIn, dates.checkOut),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [dates, bookings, blockedDays],
  );

  // Revalida un campo con los valores actuales del form. `onlyIfShown` evita
  // mostrar errores en campos que el anfitrión todavía no tocó.
  const revalidateField = (name, form, onlyIfShown = false) => {
    const errors = validateFields(readForm(new FormData(form)));
    setFieldErrors((current) => {
      if (onlyIfShown && !current[name]) return current;
      return { ...current, [name]: errors[name] || "" };
    });
  };

  // Al escribir, si el campo ya mostraba un error se recalcula en vivo para que
  // desaparezca apenas se corrige (sin esperar a reintentar el alta).
  const onFieldChange = (name) => (event) => {
    setError("");
    revalidateField(name, event.currentTarget.form, true);
  };

  // Al salir del campo se valida: el aviso aparece debajo del input.
  const onFieldBlur = (name) => (event) => revalidateField(name, event.currentTarget.form);

  const setDate = (key) => (event) => {
    setDates((current) => ({ ...current, [key]: event.target.value }));
    setError("");
    revalidateField(key, event.currentTarget.form, true);
  };

  // El total no se escribe a mano: son las noches por el precio del alojamiento.
  // La moneda sale del alojamiento y los adicionales que el huésped elija en el
  // onboarding se suman después.
  const nights =
    dates.checkIn && dates.checkOut && dates.checkOut > dates.checkIn
      ? daysBetween(dates.checkIn, dates.checkOut)
      : 0;
  const pricePerNight = Number(lodging.pricePerNight) || 0;
  const estimatedTotal = nights * pricePerNight;

  const close = () => {
    setError("");
    setFieldErrors({});
    setDates({ checkIn: "", checkOut: "" });
    onClose();
  };

  const submit = async (event) => {
    event.preventDefault();
    if (saving) return; // evita dobles envíos por clicks repetidos

    const form = event.currentTarget;
    const data = {
      ...readForm(new FormData(form)),
      // Las reservas nuevas siempre nacen pendientes. El estado se cambia
      // después, desde el detalle de la reserva.
      status: "PENDING",
    };

    // Errores de los campos: cada uno se muestra debajo de su input.
    const errors = validateFields(data);
    setFieldErrors(errors);
    const firstInvalid = FIELD_ORDER.find((key) => errors[key]);
    if (firstInvalid) {
      form.elements[firstInvalid]?.focus();
      return;
    }

    if (!pricePerNight) {
      return setError("Definí el precio por noche del alojamiento para poder calcular el total.");
    }

    // Requisitos previos del alojamiento (bloquean el alta).
    if (requirements.length) {
      setError(`Para crear una reserva primero: ${requirements.map((item) => item.label).join(", ")}.`);
      requirementsRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }
    // Disponibilidad en las fechas seleccionadas (chequeo final con los datos del form).
    const conflictMsg = findConflict(data.checkIn, data.checkOut);
    if (conflictMsg) return setError(conflictMsg);

    setError("");
    setSaving(true);
    try {
      const booking = await actions.createBooking({
        ...data,
        // Calculados, no escritos: noches × precio por noche y la moneda del
        // alojamiento. Los adicionales se suman cuando el huésped los elige.
        total: daysBetween(data.checkIn, data.checkOut) * pricePerNight,
        currency: lodging.currency,
      });
      onCreated?.(booking);
      close();
    } catch (err) {
      // Error real del backend (el módulo todavía no implementado cae solo al
      // store local y no llega acá).
      setError(err?.message || "No se pudo crear la reserva. Intentá nuevamente.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={close}
      title="Nueva reserva"
      subtitle="Registrá una reserva recibida por WhatsApp, teléfono o en persona."
    >
      <form onSubmit={submit} className="grid gap-4" noValidate>
        {/* Alerta con accesos directos a donde se completa cada dato faltante */}
        <div ref={requirementsRef}>
          <BookingRequirementsAlert
            compact
            requirements={requirements}
            intro="Completalos en el panel antes de crear la reserva."
          />
        </div>

        <Field
          label="Nombre del huésped"
          name="guestName"
          placeholder="Ej. María González"
          autoFocus={requirements.length === 0}
          onChange={onFieldChange("guestName")}
          onBlur={onFieldBlur("guestName")}
          error={fieldErrors.guestName}
          aria-invalid={Boolean(fieldErrors.guestName)}
        />
        <Field
          label="Correo del huésped"
          name="guestEmail"
          type="email"
          placeholder="maria@example.com"
          onChange={onFieldChange("guestEmail")}
          onBlur={onFieldBlur("guestEmail")}
          error={fieldErrors.guestEmail}
          aria-invalid={Boolean(fieldErrors.guestEmail)}
        />
        <Field
          label="Teléfono del huésped"
          name="guestPhone"
          type="tel"
          inputMode="tel"
          placeholder="Ej. +54 9 11 5555 0101"
          hint="Lo usamos para coordinar la llegada y queda precargado en su ficha."
          onChange={onFieldChange("guestPhone")}
          onBlur={onFieldBlur("guestPhone")}
          error={fieldErrors.guestPhone}
          aria-invalid={Boolean(fieldErrors.guestPhone)}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="Ingreso"
            name="checkIn"
            type="date"
            min={today}
            value={dates.checkIn}
            onChange={setDate("checkIn")}
            onBlur={onFieldBlur("checkIn")}
            error={fieldErrors.checkIn}
            aria-invalid={Boolean(fieldErrors.checkIn)}
          />
          <Field
            label="Salida"
            name="checkOut"
            type="date"
            min={today}
            value={dates.checkOut}
            onChange={setDate("checkOut")}
            onBlur={onFieldBlur("checkOut")}
            error={fieldErrors.checkOut}
            aria-invalid={Boolean(fieldErrors.checkOut)}
          />
        </div>

        {/* Disponibilidad en vivo de las fechas elegidas */}
        {conflict && <Alert>{conflict}</Alert>}

        {/* Sin selector de estado: toda reserva nueva nace pendiente y se
            confirma después desde el detalle. */}
        <Field
          label="Huéspedes"
          name="guests"
          type="number"
          min="1"
          defaultValue={2}
          className="sm:max-w-[calc(50%-0.5rem)]"
          onChange={onFieldChange("guests")}
          onBlur={onFieldBlur("guests")}
          error={fieldErrors.guests}
          aria-invalid={Boolean(fieldErrors.guests)}
        />

        {/* Total calculado: noches × precio por noche del alojamiento. No se
            escribe a mano; los adicionales se suman en el onboarding. */}
        <div className="rounded-xl border border-line bg-cloud px-3.5 py-3">
          {nights > 0 ? (
            <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
              <span className="text-muted">
                {plural(nights, "noche", "noches")} × {formatMoney(pricePerNight, lodging.currency)}
              </span>
              <span className="text-base font-extrabold text-ink">
                {formatMoney(estimatedTotal, lodging.currency)}
              </span>
            </div>
          ) : (
            <p className="text-xs text-muted">
              El total se calcula solo: noches × precio por noche del alojamiento.
            </p>
          )}
          <p className="mt-1 text-[11px] text-muted">
            Los servicios que el huésped sume en su ficha de llegada se agregan a este total.
          </p>
        </div>

        {error && <Alert>{error}</Alert>}

        <div className="flex flex-wrap justify-end gap-2">
          <Button type="button" variant="ghost" onClick={close}>
            Cancelar
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Guardando…" : "Crear reserva"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

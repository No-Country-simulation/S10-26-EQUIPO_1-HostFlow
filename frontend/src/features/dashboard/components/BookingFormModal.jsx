import { useMemo, useState } from "react";
import { Link } from "react-router";
import { Alert, Button, Field } from "@components/ui";
import Modal from "@components/ui/Modal";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { CURRENCIES, isEmail, toISODate } from "@utils/format";

// Alta de una reserva desde el panel del anfitrión (entregable C).
// Requisitos previos (spec): antes de crear una reserva el alojamiento debe
// tener al menos 1 método de pago, el logo subido, la descripción de
// habitaciones/baños/capacidad y disponibilidad en las fechas elegidas.
export function BookingFormModal({ open, onClose, onCreated }) {
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const payments = useStore((s) => s.payments);
  const bookings = useStore((s) => s.bookings);
  const blockedDays = useStore((s) => s.blockedDays);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  // Fechas elegidas en el form: alimentan el chequeo de disponibilidad en vivo.
  const [dates, setDates] = useState({ checkIn: "", checkOut: "" });

  const today = toISODate();

  // Configuración mínima del alojamiento (link lleva a la pantalla que lo carga).
  const missing = useMemo(() => {
    const list = [];
    if (!payments?.methods?.some((m) => m.enabled)) {
      list.push({ label: "al menos 1 método de pago habilitado", to: "/panel/pagos" });
    }
    if (!lodging.logo) {
      list.push({ label: "el logo del alojamiento", to: "/panel/perfil" });
    }
    if (!(lodging.rooms > 0) || lodging.bathrooms == null || !(lodging.capacity > 0)) {
      list.push({ label: "habitaciones, baños y capacidad", to: "/panel/alojamiento" });
    }
    return list;
  }, [payments, lodging]);

  // Disponibilidad: solape con otras reservas o días bloqueados en [checkIn, checkOut).
  const findConflict = (checkIn, checkOut) => {
    if (!checkIn || !checkOut || checkOut <= checkIn) return "";
    if (blockedDays.some((d) => d >= checkIn && d < checkOut)) {
      return "Hay días bloqueados en esas fechas: revisá la pantalla de Disponibilidad.";
    }
    const clash = bookings.find((b) => b.checkIn < checkOut && b.checkOut > checkIn);
    if (clash) return `Esas fechas ya tienen una reserva de ${clash.guestName}.`;
    return "";
  };

  const conflict = useMemo(
    () => findConflict(dates.checkIn, dates.checkOut),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [dates, bookings, blockedDays],
  );

  const setDate = (key) => (event) => setDates((current) => ({ ...current, [key]: event.target.value }));

  const close = () => {
    setError("");
    setDates({ checkIn: "", checkOut: "" });
    onClose();
  };

  const submit = async (event) => {
    event.preventDefault();
    if (saving) return; // evita dobles envíos por clicks repetidos

    const form = new FormData(event.currentTarget);
    const data = {
      guestName: String(form.get("guestName") || "").trim(),
      guestEmail: String(form.get("guestEmail") || "").trim(),
      checkIn: String(form.get("checkIn") || ""),
      checkOut: String(form.get("checkOut") || ""),
      guests: Number(form.get("guests")),
      total: Number(form.get("total")),
      currency: String(form.get("currency") || lodging.currency),
      status: String(form.get("status") || "PENDING"),
      additional: String(form.get("additional") || "").trim(),
    };

    if (!data.guestName) return setError("Ingresá el nombre del huésped.");
    if (!data.guestEmail || !isEmail(data.guestEmail)) return setError("Ingresá un correo válido.");
    if (!data.checkIn) return setError("Elegí la fecha de ingreso.");
    if (!data.checkOut || data.checkOut <= data.checkIn) return setError("La salida debe ser posterior al ingreso.");
    if (!data.guests || data.guests < 1) return setError("La cantidad de huéspedes debe ser al menos 1.");
    if (!data.total || data.total <= 0) return setError("Ingresá el total de la reserva.");

    // Requisitos previos del alojamiento (bloquean el alta).
    if (missing.length) {
      return setError(`Para crear una reserva primero: ${missing.map((item) => item.label).join(", ")}.`);
    }
    // Disponibilidad en las fechas seleccionadas (chequeo final con los datos del form).
    const conflictMsg = findConflict(data.checkIn, data.checkOut);
    if (conflictMsg) return setError(conflictMsg);

    setError("");
    setSaving(true);
    try {
      const booking = actions.createBooking(data);
      onCreated?.(booking);
      close();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Nueva reserva"
      subtitle="Registrá una reserva recibida por WhatsApp, teléfono o en persona."
    >
      <form onSubmit={submit} className="grid gap-4" noValidate>
        {/* Alertas del sistema (componente Alert): requisitos previos del alojamiento */}
        {missing.length > 0 && (
          <Alert tone="info">
            <span className="flex flex-col gap-1">
              <span>Antes de crear una reserva, completá en el panel:</span>
              {missing.map((item) => (
                <Link key={item.label} to={item.to} className="font-bold underline underline-offset-2">
                  {item.label} →
                </Link>
              ))}
            </span>
          </Alert>
        )}

        <Field label="Nombre del huésped" name="guestName" placeholder="Ej. María González" autoFocus />
        <Field label="Correo del huésped" name="guestEmail" type="email" placeholder="maria@example.com" />

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Ingreso" name="checkIn" type="date" min={today} value={dates.checkIn} onChange={setDate("checkIn")} />
          <Field label="Salida" name="checkOut" type="date" min={today} value={dates.checkOut} onChange={setDate("checkOut")} />
        </div>

        {/* Disponibilidad en vivo de las fechas elegidas */}
        {conflict && <Alert>{conflict}</Alert>}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Huéspedes" name="guests" type="number" min="1" defaultValue={2} />
          <Field as="select" label="Estado" name="status" defaultValue="PENDING">
            <option value="PENDING">Pendiente</option>
            <option value="CONFIRMED">Confirmada</option>
          </Field>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Total" name="total" type="number" min="0" step="1000" placeholder="Ej. 255000" />
          <Field as="select" label="Moneda" name="currency" defaultValue={lodging.currency}>
            {Object.values(CURRENCIES).map((c) => (
              <option key={c.code} value={c.code}>
                {c.label} ({c.code})
              </option>
            ))}
          </Field>
        </div>

        <Field label="Servicio adicional (opcional)" name="additional" placeholder="Ej. Desayuno × 3" />

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

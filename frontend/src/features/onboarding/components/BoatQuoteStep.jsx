import { Field } from "@components/ui";
import { toISODate } from "@utils/format";

// Paso de cotización de transporte en lancha remis.
//
// El formulario está SIEMPRE visible (no hay tarjetas Sí/No): el huésped carga
// los datos del viaje y, si no lo necesita, usa el botón "Omitir" del pie. La
// sola carga de un dato marca `wanted: true` y `decided: true`.
//
// Dos caminos distintos:
//   · "Continuar" pide la cotización → el formulario se valida campo por campo
//     y no se avanza hasta que esté completo y correcto.
//   · "Omitir" saltea el paso sin cotización ni validaciones.
// El JSON se genera recién al CONFIRMAR LLEGADA, así una solicitud no se
// duplica por clicks repetidos.
export default function BoatQuoteStep({ quote, onChange, booking, lodging, errors = {} }) {
  const set = (patch) => onChange({ ...quote, decided: true, wanted: true, ...patch });
  const setField = (key) => (event) => set({ [key]: event.target.value });

  return (
    <div className="grid gap-5">
      <div>
        <h2 className="text-xl font-extrabold text-ink">Traslado en lancha remis</h2>
        <p className="mt-1.5 text-sm text-muted">
          Si llegás por el río, completá los datos y te preparamos una cotización hasta{" "}
          <span className="font-semibold text-ink">{lodging.location}</span>. Es opcional: si no la
          necesitás, tocá <span className="font-semibold text-ink">Omitir</span> y seguís con tu llegada.
        </p>
        <p className="mt-1 text-xs text-muted">
          Si tocás <span className="font-semibold text-ink">Continuar</span>, completá todos los datos del viaje:
          los necesitamos para cotizar.
        </p>
      </div>

      <div className="grid gap-4">
        <Field
          label="Lugar de embarque"
          name="pickupPlace"
          value={quote.pickupPlace}
          onChange={setField("pickupPlace")}
          placeholder="Ej. Terminal fluvial de Tigre"
          hint="Puerto o muelle donde te tomamos."
          error={errors.pickupPlace}
          aria-invalid={Boolean(errors.pickupPlace)}
          required
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="Fecha del traslado"
            name="pickupDate"
            type="date"
            min={booking?.checkIn || toISODate()}
            value={quote.pickupDate}
            onChange={setField("pickupDate")}
            error={errors.pickupDate}
            aria-invalid={Boolean(errors.pickupDate)}
            required
          />
          <Field
            label="Hora"
            name="pickupTime"
            type="time"
            value={quote.pickupTime}
            onChange={setField("pickupTime")}
            error={errors.pickupTime}
            aria-invalid={Boolean(errors.pickupTime)}
            required
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="Pasajeros"
            name="passengers"
            type="number"
            min="1"
            value={quote.passengers}
            onChange={setField("passengers")}
            hint={`La reserva es para ${booking?.guests || 1} huésped(es).`}
            error={errors.passengers}
            aria-invalid={Boolean(errors.passengers)}
            required
          />
          <Field
            label="Equipaje"
            name="baggage"
            type="number"
            min="0"
            value={quote.baggage}
            onChange={setField("baggage")}
            hint="Cantidad de valijas grandes."
            error={errors.baggage}
            aria-invalid={Boolean(errors.baggage)}
          />
        </div>
        <Field
          label="Lugar de destino"
          name="dropoffPlace"
          value={quote.dropoffPlace}
          onChange={setField("dropoffPlace")}
          placeholder={`Ej. ${lodging.location}`}
          hint="Muelle o lugar donde te dejamos."
          error={errors.dropoffPlace}
          aria-invalid={Boolean(errors.dropoffPlace)}
          required
        />
        <Field
          label="Notas (opcional)"
          as="textarea"
          name="notes"
          value={quote.notes}
          onChange={setField("notes")}
          placeholder="Ej. Llegamos con una silla de ruedas plegable."
        />
      </div>

      <p className="text-xs text-muted">
        La cotización es una solicitud: no implica una reserva ni un cobro. Te respondemos por WhatsApp.
      </p>
    </div>
  );
}

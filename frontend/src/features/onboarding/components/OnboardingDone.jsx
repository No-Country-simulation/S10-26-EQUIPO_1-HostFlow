// Pantalla final del onboarding: llegada registrada, estado del pago y datos
// de la solicitud de lancha (si la hubo).
import { Alert, Badge, Button } from "@components/ui";
import { Check, Copy, Ship } from "lucide-react";
import { formatDate, plural } from "@utils/format";
import { DEMO_MODE } from "@lib/config/env";

export default function OnboardingDone({ booking, lodging, paid, copyJson, copied }) {
  const quotePayload = booking.boatQuote?.payload;

  return (
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
  );
}

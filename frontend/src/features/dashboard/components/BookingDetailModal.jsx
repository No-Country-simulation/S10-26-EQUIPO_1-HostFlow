import { useState } from "react";
import { ArrowRight, Check, Copy, Landmark, Ship, Users } from "lucide-react";
import { Alert, Badge, Button, Switch } from "@components/ui";
import Modal from "@components/ui/Modal";
import { PaymentStatusBadge, StatusBadge } from "./BookingSummaryCard";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { ONBOARDING_LINK_TTL_MS, isOnboardingLinkExpired } from "@lib/state/store";
import { guardAction } from "../lib/guardAction";
import { DEMO_MODE } from "@lib/config/env";
import { bookingGrandTotal, bookingServices, bookingServicesTotal, daysBetween, formatDate, formatMoney, plural, serviceLineDetail, serviceLineTotal } from "@utils/format";

export function BookingDetailModal({ booking, open, onClose }) {
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const bankDetails = useStore((s) => s.payments.bankDetails);
  const [copied, setCopied] = useState(false);

  if (!booking) return null;

  const nights = daysBetween(booking.checkIn, booking.checkOut);
  // Huéspedes de la reserva (los servicios por persona se cobran por cada uno).
  const guests = Math.max(1, Number(booking.guests) || 1);
  // Total REAL de la reserva: alojamiento + adicionales registrados por el huésped.
  const selected = bookingServices(booking);
  const servicesTotal = bookingServicesTotal(booking);
  const orderTotal = bookingGrandTotal(booking);
  const hasBankDetails = Boolean(bankDetails.holder || bankDetails.alias || bankDetails.cbu);

  // Enlace de llegada del huésped (onboarding B2C): identifica alojamiento + reserva.
  const onboardingUrl = `${window.location.origin}${lodging.onboardingPath || "/llegada"}/${lodging.id}/${booking.id}`;

  // Vigencia del enlace: dura 24 h desde su emisión.
  const issuedAt = booking.onboardingLinkIssuedAt ? Date.parse(booking.onboardingLinkIssuedAt) : NaN;
  const linkExpired = isOnboardingLinkExpired(booking);
  const hoursLeft = Number.isNaN(issuedAt)
    ? null
    : Math.max(0, Math.ceil((issuedAt + ONBOARDING_LINK_TTL_MS - Date.now()) / 3_600_000));

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(onboardingUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2200);
    } catch {
      setCopied(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={booking.guestName} subtitle={booking.guestEmail}>
      <div className="grid gap-4">
        {/* Estado de la reserva y del pago */}
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={booking.status} />
          <PaymentStatusBadge status={booking.paymentStatus} />
          {booking.onboarding?.completedAt && <Badge tone="teal">Onboarding completo</Badge>}
          {booking.boatQuote?.status === "REQUESTED" && (
            <Badge tone="lime">
              <Ship size={11} /> Lancha solicitada
            </Badge>
          )}
          {booking.boatQuote?.status === "DECLINED" && <Badge tone="neutral">Lancha declinada</Badge>}
        </div>

        {/* Fechas y huéspedes (min-w-0: como grid items su min-width auto empuja el layout en pantallas angostas) */}
        <div className="grid min-w-0 gap-3 rounded-2xl border border-line bg-cloud p-4 text-sm">
          <div className="flex min-w-0 flex-wrap items-center justify-between gap-2">
            <span className="font-semibold text-ink">
              {formatDate(booking.checkIn)} <ArrowRight className="inline w-3.5" /> {formatDate(booking.checkOut)}
            </span>
            <span className="text-muted">{plural(nights, "noche", "noches")}</span>
          </div>
          <div className="flex min-w-0 flex-wrap items-center justify-between gap-2">
            <span className="inline-flex items-center gap-1.5 text-muted">
              <Users size={15} /> {plural(booking.guests, "huésped", "huéspedes")}
            </span>
            {booking.additional && (
              <span className="rounded-full bg-teal-soft px-2.5 py-1 text-xs font-semibold text-teal-strong">
                {booking.additional}
              </span>
            )}
          </div>
        </div>

        {/* Estado del pago (entregable D) */}
        <div className="min-w-0 rounded-2xl border border-line p-4">
          <div className="flex flex-wrap items-start justify-between gap-3 sm:flex-nowrap">
            <div className="min-w-0 flex-1">
              <p className="text-sm font-bold text-ink">Estado del pago</p>
              <p className="text-xs text-muted">Marcá la reserva como pagada cuando recibas la transferencia.</p>
            </div>
            <Switch
              checked={booking.paymentStatus === "PAID"}
              onChange={(next) => guardAction("setPaymentStatus", actions.setPaymentStatus(booking.id, next ? "PAID" : "PENDING"))}
              label={booking.paymentStatus === "PAID" ? "Pagado" : "Pendiente"}
            />
          </div>
        </div>

        {/* Información de pago (entregable E): resumen del pedido + datos bancarios */}
        <div className="min-w-0 rounded-2xl border border-line p-4">
          <p className="mb-3 text-sm font-bold text-ink">Información de pago</p>

          {/* min-w-0 en las filas: su min-width auto como items de grid empuja el layout en pantallas angostas */}
          <div className="grid gap-2 text-sm">
            <div className="flex min-w-0 items-center justify-between gap-3">
              <span className="min-w-0 truncate text-muted">Alojamiento · {plural(nights, "noche", "noches")}</span>
              <span className="shrink-0 font-semibold text-ink">{formatMoney(booking.total, booking.currency)}</span>
            </div>
            {selected.map((item, index) => (
              <div key={item.serviceId || index} className="flex min-w-0 items-center justify-between gap-3">
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
              <span className="font-bold text-ink">
                Total{servicesTotal > 0 ? " (alojamiento + adicionales)" : ""}
              </span>
              <span className="shrink-0 font-extrabold text-ink">{formatMoney(orderTotal, booking.currency)}</span>
            </div>
          </div>

          <div className="mt-4 grid min-w-0 gap-2 rounded-xl bg-cloud p-3.5 text-sm">
            <p className="flex items-center gap-2 font-bold text-ink">
              <Landmark size={16} className="text-teal-strong" /> Pago por transferencia
            </p>
            {hasBankDetails ? (
              <>
                {bankDetails.holder && (
                  <p className="text-muted">
                    <span className="font-semibold text-ink">Titular:</span> {bankDetails.holder}
                  </p>
                )}
                {bankDetails.alias && (
                  <p className="text-muted">
                    <span className="font-semibold text-ink">Alias:</span> {bankDetails.alias}
                  </p>
                )}
                {bankDetails.cbu && (
                  <p className="text-muted">
                    <span className="font-semibold text-ink">CBU:</span> {bankDetails.cbu}
                  </p>
                )}
                {bankDetails.notes && <p className="text-muted">{bankDetails.notes}</p>}
              </>
            ) : (
              <p className="text-xs text-muted">
                Todavía no cargaste los datos bancarios. Completalos en{" "}
                <span className="font-semibold text-ink">Pagos → Datos para transferencia</span> para que el huésped
                pueda pagar.
              </p>
            )}
          </div>
        </div>

        {/* Cotización de lancha (si existe) */}
        {booking.boatQuote?.status === "REQUESTED" && (
          <details className="rounded-2xl border border-line p-4 text-sm">
            <summary className="cursor-pointer font-bold text-ink">Solicitud de cotización de lancha</summary>
            <pre className="mt-3 overflow-x-auto rounded-xl bg-cloud p-3 text-xs text-ink-soft">
              {JSON.stringify(booking.boatQuote.payload, null, 2)}
            </pre>
            <p className="mt-2 text-xs text-muted">
              JSON generado para la API de transporte: la integración externa y la notificación por correo están
              pendientes de backend.
            </p>
          </details>
        )}

        {/* Enlace de llegada del huésped (min-w-0: su min-width auto como grid item empuja el layout en pantallas angostas) */}
        <div className="min-w-0 rounded-2xl border border-line p-4">
          <p className="text-sm font-bold text-ink">Enlace de llegada del huésped</p>
          <p className="mt-1 text-xs text-muted">
            El huésped completa su onboarding desde este enlace, con la identidad de {lodging.name}.
          </p>
          <div className="mt-2.5 flex flex-wrap items-center gap-2">
            <code className="min-w-0 flex-1 truncate rounded-xl border border-line bg-cloud px-3 py-2 text-xs text-ink-soft">
              {onboardingUrl}
            </code>
            <Button type="button" variant="secondary" icon={copied ? Check : Copy} onClick={copyLink} className="min-h-[40px] shrink-0 px-4">
              {copied ? "Copiado" : "Copiar"}
            </Button>
          </div>
          {/* Vigencia: el enlace dura 24 h; se puede renovar */}
          <div className="mt-2.5 flex flex-wrap items-center gap-2">
            {linkExpired ? (
              <Badge tone="pink">Enlace expirado</Badge>
            ) : hoursLeft != null ? (
              <Badge tone="lime">Válido por {plural(hoursLeft, "hora", "horas")} más</Badge>
            ) : null}
            <Button
              type="button"
              variant="ghost"
              onClick={() => guardAction("regenerateOnboardingLink", actions.regenerateOnboardingLink(booking.id))}
              className="min-h-[34px] px-3 text-xs"
            >
              Renovar 24 h
            </Button>
          </div>
          <p className="mt-1.5 text-[11px] text-muted">
            El enlace vence 24 h después de emitirse. Al renovarlo, el huésped dispone de 24 h nuevas.
          </p>
          {DEMO_MODE && (
            <p className="text-[11px] text-muted">
              Modo demo: los datos se guardan en <span className="font-semibold text-ink">este navegador</span>. El
              huésped que abra el enlace en otra pestaña de este mismo equipo verá tus cambios (pago, reserva, etc.);
              en otro celular o computadora no viajan hasta que exista backend.
            </p>
          )}
        </div>

        {!DEMO_MODE && (
          <Alert tone="info">
            El backend aún no expone el módulo de reservas: los cambios de esta pantalla se guardan localmente en
            este navegador.
          </Alert>
        )}

        {/* Acciones de la reserva */}
        <div className="flex flex-wrap gap-2">
          {booking.status === "PENDING" && (
            <Button
              type="button"
              icon={Check}
              onClick={() => guardAction("setBookingStatus", actions.setBookingStatus(booking.id, "CONFIRMED"))}
            >
              Confirmar reserva
            </Button>
          )}
          {booking.status !== "CANCELLED" && booking.status !== "COMPLETED" && (
            <Button
              type="button"
              variant="secondary"
              onClick={() => guardAction("setBookingStatus", actions.setBookingStatus(booking.id, "CANCELLED"))}
            >
              Cancelar reserva
            </Button>
          )}
          <Button type="button" variant="ghost" onClick={onClose}>
            Cerrar
          </Button>
        </div>
      </div>
    </Modal>
  );
}

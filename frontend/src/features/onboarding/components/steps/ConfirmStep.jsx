// Paso 5 del onboarding: resumen, total y pago en una sola pantalla.
import { Alert, Field, WhatsAppIcon } from "@components/ui";
import { Check, Copy, CreditCard, Landmark } from "lucide-react";
import { cn, formatDate, formatMoney, plural, serviceLineDetail, serviceLineTotal } from "@utils/format";
import { DEMO_MODE } from "@lib/config/env";
import { fullName } from "../../lib/onboardingForm";
import PriceEquivalents from "../PriceEquivalents";
import StepHeading from "../StepHeading";

export default function ConfirmStep({
  booking,
  guest,
  health,
  allergyList,
  quote,
  lodging,
  nights,
  guests,
  orderTotal,
  selected,
  showConversion,
  walletCodes,
  waLink,
  paymentMessage,
  waDigits,
  activePaymentMethods,
  chosenMethod,
  paymentMethodId,
  setPaymentMethodId,
  copyMethodCredentials,
  copiedMethodId,
  hasBankDetails,
  bankDetails,
  error,
}) {
  return (
    <div className="card grid gap-4 p-5 animate-rise sm:p-6">
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
          <span className="font-semibold text-ink">Huésped:</span> {fullName(guest)} · {guest.guestEmail}
          {guest.guestPhone && ` · ${guest.guestPhone}`}
        </p>
        <p>
          <span className="font-semibold text-ink">Estadía:</span> {formatDate(booking.checkIn)} →{" "}
          {formatDate(booking.checkOut)} ({plural(nights, "noche", "noches")})
        </p>
        {allergyList.length > 0 && (
          <p>
            <span className="font-semibold text-ink">Alergias:</span> {allergyList.join(", ")}
          </p>
        )}
        {health.preferences && (
          <p>
            <span className="font-semibold text-ink">Preferencias:</span> {health.preferences}
          </p>
        )}
        <p>
          <span className="font-semibold text-ink">Traslado en lancha:</span>{" "}
          {quote.wanted
            ? `Sí, cotizar (${quote.pickupPlace || "lugar a confirmar"}${
                quote.pickupDate ? ` · ${formatDate(quote.pickupDate)}` : ""
              }${quote.pickupTime ? ` ${quote.pickupTime}` : ""})`
            : "No solicitado"}
        </p>
      </div>

      {/* Pedido + total + comprobante por WhatsApp: todo junto, sin otro paso */}
      <div className="grid gap-3 rounded-2xl border border-line p-4 text-sm">
        <div className="flex flex-wrap items-center justify-between gap-1.5">
          <p className="font-bold text-ink">Resumen del pedido</p>
          {showConversion && <span className="text-[11px] font-semibold text-muted">Conversión de referencia</span>}
        </div>

        {/* min-w-0 en las filas: como grid items su min-width auto empuja el layout en pantallas angostas */}
        <div className="flex min-w-0 items-start justify-between gap-3">
          <span className="min-w-0 truncate text-muted">Alojamiento · {plural(nights, "noche", "noches")}</span>
          <span className="shrink-0 text-right">
            <span className="block font-semibold text-ink">{formatMoney(booking.total, booking.currency)}</span>
            {showConversion && <PriceEquivalents amount={booking.total} currency={booking.currency} codes={walletCodes} />}
          </span>
        </div>
        {selected.map((item) => (
          <div key={item.serviceId} className="flex min-w-0 items-start justify-between gap-3">
            <span className="min-w-0 truncate text-muted">
              {item.name}
              {serviceLineDetail(item, nights, guests) && (
                <span className="ml-1.5 text-[11px] font-semibold text-muted">{serviceLineDetail(item, nights, guests)}</span>
              )}
            </span>
            <span className="shrink-0 text-right">
              <span className="block font-semibold text-ink">
                {formatMoney(serviceLineTotal(item, nights, guests), booking.currency)}
              </span>
              {showConversion && (
                <PriceEquivalents
                  amount={serviceLineTotal(item, nights, guests)}
                  currency={booking.currency}
                  codes={walletCodes}
                />
              )}
            </span>
          </div>
        ))}
        <div className="mt-1 flex min-w-0 items-start justify-between gap-3 border-t border-line pt-2.5">
          <span className="font-bold text-ink">Total</span>
          <span className="shrink-0 text-right">
            <span className="block text-base font-extrabold text-ink">{formatMoney(orderTotal, booking.currency)}</span>
            {showConversion && <PriceEquivalents amount={orderTotal} currency={booking.currency} codes={walletCodes} />}
          </span>
        </div>

        {/* Captura del pago: en la misma pantalla que el total */}
        <div className="mt-1 grid gap-2 rounded-xl border border-lime/50 bg-lime-soft/60 p-3.5 text-center">
          <p className="text-xs font-bold text-ink">Después de transferir, mandá el comprobante</p>
          <a
            href={waLink(paymentMessage)}
            target="_blank"
            rel="noreferrer"
            className="mx-auto inline-flex items-center gap-2 rounded-xl bg-lime px-4 py-2.5 text-sm font-bold text-lime-ink transition hover:opacity-90"
          >
            <WhatsAppIcon size={16} />
            Enviar capture al WhatsApp
          </a>
          <p className="text-[11px] text-muted">
            {waDigits ? (
              <>
                WhatsApp del alojamiento: <span className="font-semibold text-ink">{lodging.phone}</span>
              </>
            ) : (
              "El alojamiento todavía no cargó su WhatsApp."
            )}
          </p>
        </div>
      </div>

      {/* Método de pago: se elige en un combo y solo se muestra el elegido */}
      <div className="grid gap-3 rounded-2xl bg-cloud/70 p-4">
        <div className="flex flex-wrap items-center justify-between gap-1.5">
          <p className="font-bold text-ink">Método de pago</p>
          <span className="text-[11px] font-semibold text-muted">Elegí uno para ver los datos</span>
        </div>

        {activePaymentMethods.length > 0 ? (
          <>
            <Field
              as="select"
              label="¿Cómo vas a pagar?"
              name="paymentMethod"
              value={chosenMethod?.id || ""}
              onChange={(event) => setPaymentMethodId(event.target.value)}
            >
              {activePaymentMethods.map((method) => (
                <option key={method.id} value={method.id}>
                  {method.label}
                </option>
              ))}
            </Field>

            {chosenMethod && (
              <div className="grid gap-3 rounded-xl border border-line bg-paper p-3.5 shadow-sm animate-rise">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="grid size-7 place-items-center rounded-lg bg-teal-soft text-teal-strong">
                      {chosenMethod.type === "BANK" ? <Landmark size={14} /> : <CreditCard size={14} />}
                    </span>
                    <span className="text-sm font-bold text-ink">{chosenMethod.label}</span>
                  </div>
                  {chosenMethod.type === "WALLET" && (
                    <span className="rounded-full bg-lime-soft px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-lime-ink">
                      Billetera digital
                    </span>
                  )}
                </div>

                <div className="grid gap-0.5 text-xs text-muted">
                  {chosenMethod.keys?.holder && (
                    <p>
                      <span className="font-semibold text-ink">Titular:</span> {chosenMethod.keys.holder}
                    </p>
                  )}
                  {chosenMethod.keys?.alias && (
                    <p>
                      <span className="font-semibold text-ink">Alias:</span>{" "}
                      <code className="font-mono text-teal-strong">{chosenMethod.keys.alias}</code>
                    </p>
                  )}
                  {chosenMethod.keys?.cbu && (
                    <p>
                      <span className="font-semibold text-ink">CBU:</span>{" "}
                      <code className="font-mono text-teal-strong">{chosenMethod.keys.cbu}</code>
                    </p>
                  )}
                  {chosenMethod.keys?.cvu && (
                    <p>
                      <span className="font-semibold text-ink">CVU:</span>{" "}
                      <code className="font-mono text-teal-strong">{chosenMethod.keys.cvu}</code>
                    </p>
                  )}
                  {chosenMethod.keys?.email && (
                    <p>
                      <span className="font-semibold text-ink">Correo:</span> {chosenMethod.keys.email}
                    </p>
                  )}
                  {chosenMethod.keys?.paypalMe && (
                    <p>
                      <span className="font-semibold text-ink">PayPal.Me:</span> {chosenMethod.keys.paypalMe}
                    </p>
                  )}
                  {chosenMethod.keys?.publicKey && (
                    <p>
                      <span className="font-semibold text-ink">Llave pública:</span> {chosenMethod.keys.publicKey}
                    </p>
                  )}
                  {chosenMethod.notes && <p className="italic text-muted">{chosenMethod.notes}</p>}
                </div>

                {/* En billeteras digitales, el total convertido a USD/EUR */}
                {showConversion && (
                  <div className="rounded-lg bg-cloud px-3 py-2 text-xs">
                    <span className="font-semibold text-ink">Total a transferir: </span>
                    <span className="font-extrabold text-ink">{formatMoney(orderTotal, booking.currency)}</span>
                    <PriceEquivalents amount={orderTotal} currency={booking.currency} codes={walletCodes} />
                  </div>
                )}

                <button
                  type="button"
                  onClick={() => copyMethodCredentials(chosenMethod)}
                  className={cn(
                    "inline-flex cursor-pointer items-center justify-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-bold transition-all",
                    copiedMethodId === chosenMethod.id
                      ? "border-success bg-success-soft text-success"
                      : "border-line-strong bg-cloud text-ink hover:border-teal hover:text-teal-strong",
                  )}
                >
                  {copiedMethodId === chosenMethod.id ? (
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
            )}
          </>
        ) : hasBankDetails ? (
          <div className="rounded-xl border border-line bg-paper p-3 text-xs text-muted">
            {bankDetails.holder && <p><span className="font-semibold text-ink">Titular:</span> {bankDetails.holder}</p>}
            {bankDetails.alias && <p><span className="font-semibold text-ink">Alias:</span> {bankDetails.alias}</p>}
            {bankDetails.cbu && <p><span className="font-semibold text-ink">CBU:</span> {bankDetails.cbu}</p>}
            {bankDetails.notes && <p>{bankDetails.notes}</p>}
          </div>
        ) : (
          <div className="rounded-xl border border-danger/35 bg-danger-soft p-3 text-xs font-semibold text-danger">
            {lodging.name} todavía no tiene un método de pago configurado. Escribile por WhatsApp para coordinar
            el pago.
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
  );
}

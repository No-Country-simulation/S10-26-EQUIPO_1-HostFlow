// Paso 3 del onboarding: servicios adicionales de la estadía.
import { Check, ClipboardList } from "lucide-react";
import { cn, formatMoney, plural, serviceLineTotal } from "@utils/format";
import { MAX_SERVICE_QTY } from "../../lib/onboardingForm";
import StepHeading from "../StepHeading";

export default function ServicesStep({
  availableServices,
  selected,
  toggleService,
  setQuantity,
  nights,
  guests,
  servicesTotal,
  lodging,
}) {
  return (
    <div className="card grid gap-4 overflow-hidden p-5 animate-rise sm:p-6">
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
  );
}

import { formatMoney, secondaryEquivalents } from "@utils/format";

// Equivalencia de un precio en las monedas secundarias del alojamiento.
// Se usa cuando el huésped elige una billetera digital: el total está en pesos
// y necesita saber cuánto transferir en dólares o euros. Las cotizaciones son
// de referencia (demo, sin backend de tipos de cambio), por eso se aclara en el
// resumen con "Conversión de referencia".
export default function PriceEquivalents({ amount, currency = "ARS", codes = ["USD", "EUR"], className = "" }) {
  const list = secondaryEquivalents(amount, currency, codes);
  if (!list.length) return null;

  return (
    <span className={`mt-0.5 block text-[11px] font-semibold text-muted ${className}`.trim()}>
      {list.map((item) => `≈ ${formatMoney(item.amount, item.code)}`).join(" · ")}
    </span>
  );
}

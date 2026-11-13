// Lógica pura del formulario de llegada: constantes, validaciones y helpers.
//
// Fuera de la página para que el componente se ocupe del flujo y este archivo
// pueda probarse sin montar React.
import { formatDate, isEmail, toISODate } from "@utils/format";

// Alergias más frecuentes: se eligen con un toque y la respuesta se envía como
// array. "Otro" habilita un campo libre (varias alergias separadas por coma).
export const ALLERGY_OPTIONS = [
  "Gluten",
  "Lácteos",
  "Huevos",
  "Mariscos",
  "Frutos secos",
  "Soja",
  "Picaduras de insectos",
  "Medicamentos",
];
export const OTHER_ALLERGY = "Otro";

export const EMPTY_QUOTE = {
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

// Tope de unidades para un servicio de precio fijo (el huésped elige cuántos).
export const MAX_SERVICE_QTY = 20;

// Validación del paso de datos. Devuelve un mensaje por campo (vacío si está
// bien) para mostrarlo debajo de cada input, no en un aviso único al final.
// Nombre y apellido son dos campos, pero se envían como un solo `guestName`.
export const GUEST_FIELDS = ["firstName", "lastName", "guestDocument", "guestEmail", "guestPhone"];

// Orden de los campos de la lancha para enfocar el primero con error.
export const BOAT_FIELDS = ["pickupPlace", "pickupDate", "pickupTime", "passengers", "baggage", "dropoffPlace"];

const NAME_RE = /^[\p{L}][\p{L}\s.'’-]*$/u;

export function validateGuest(guest) {
  const errors = {};

  const firstName = (guest.firstName || "").trim().replace(/\s+/g, " ");
  if (!firstName) errors.firstName = "Ingresá tu nombre.";
  else if (firstName.length < 2) errors.firstName = "Escribí tu nombre completo.";
  else if (!NAME_RE.test(firstName)) errors.firstName = "Usá solo letras, espacios, puntos o guiones.";

  const lastName = (guest.lastName || "").trim().replace(/\s+/g, " ");
  if (!lastName) errors.lastName = "Ingresá tu apellido.";
  else if (lastName.length < 2) errors.lastName = "Escribí tu apellido completo.";
  else if (!NAME_RE.test(lastName)) errors.lastName = "Usá solo letras, espacios, puntos o guiones.";

  // Documento: DNI, CI o pasaporte (5 a 20 caracteres).
  const document = guest.guestDocument.trim().replace(/\s+/g, " ");
  if (!document) errors.guestDocument = "Ingresá tu DNI o pasaporte.";
  else if (!/^[0-9A-Za-z][0-9A-Za-z.\- ]{4,19}$/.test(document)) {
    errors.guestDocument = "Entre 5 y 20 caracteres: letras, números, puntos o guiones.";
  }

  const email = guest.guestEmail.trim();
  if (!email) errors.guestEmail = "Ingresá tu correo electrónico.";
  else if (!isEmail(email)) errors.guestEmail = "Ese correo no parece válido. Revisalo.";

  // Teléfono: obligatorio, para poder coordinar la llegada.
  const phone = guest.guestPhone.trim();
  if (!phone) errors.guestPhone = "Ingresá tu teléfono.";
  else {
    const digits = phone.replace(/\D/g, "");
    if (!/^[+0-9()\s.-]+$/.test(phone) || digits.length < 6 || digits.length > 15) {
      errors.guestPhone = "Revisá el número: entre 6 y 15 dígitos.";
    }
  }

  return errors;
}

// Validación del paso de lancha remis. "Continuar" pide la cotización, así que
// los datos del viaje son obligatorios aunque el formulario esté vacío. El
// único camino para saltear el paso sin cotizar es "Omitir".
export function validateBoat(quote, booking) {
  const errors = {};

  const place = (quote.pickupPlace || "").trim().replace(/\s+/g, " ");
  if (!place) errors.pickupPlace = "Indicá el lugar de embarque.";
  else if (place.length < 3) errors.pickupPlace = "Escribí el puerto o muelle de salida.";

  if (!quote.pickupDate) errors.pickupDate = "Elegí la fecha del traslado.";
  else {
    const min = booking?.checkIn || toISODate();
    if (min && quote.pickupDate < min) {
      errors.pickupDate = `No puede ser antes de tu llegada (${formatDate(min)}).`;
    }
  }

  if (!quote.pickupTime) errors.pickupTime = "Elegí la hora del traslado.";

  const passengers = Number(quote.passengers);
  if (!quote.passengers || !Number.isFinite(passengers) || passengers < 1) {
    errors.passengers = "Indicá al menos 1 pasajero.";
  } else if (!Number.isInteger(passengers)) {
    errors.passengers = "Usá un número entero de pasajeros.";
  }

  const baggage = quote.baggage === "" ? 0 : Number(quote.baggage);
  if (!Number.isFinite(baggage) || baggage < 0) errors.baggage = "Indicá 0 o más valijas.";

  const dropoff = (quote.dropoffPlace || "").trim().replace(/\s+/g, " ");
  if (!dropoff) errors.dropoffPlace = "Indicá el lugar de destino.";
  else if (dropoff.length < 3) errors.dropoffPlace = "Escribí el muelle o lugar de llegada.";

  return errors;
}

// Nombre completo tal como lo espera la reserva (un único dato).
export function fullName(guest) {
  return [guest.firstName, guest.lastName]
    .map((part) => String(part || "").trim().replace(/\s+/g, " "))
    .filter(Boolean)
    .join(" ");
}

// Separa el nombre guardado en la reserva para precargar los dos campos.
// La primera palabra es el nombre; el resto, el apellido.
export function splitName(value = "") {
  const parts = String(value).trim().split(/\s+/).filter(Boolean);
  if (parts.length <= 1) return { firstName: parts[0] || "", lastName: "" };
  return { firstName: parts[0], lastName: parts.slice(1).join(" ") };
}

// Limpia espacios de más y arma el payload: nombre y apellido viajan como UN
// solo `guestName` (los dos inputs son solo interfaz).
export function cleanGuest(guest) {
  return {
    guestName: fullName(guest),
    guestDocument: guest.guestDocument.trim().replace(/\s+/g, " "),
    guestEmail: guest.guestEmail.trim(),
    guestPhone: guest.guestPhone.trim(),
  };
}

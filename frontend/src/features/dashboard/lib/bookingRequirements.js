// Requisitos previos para crear una reserva (entregable C).
// Cada requisito faltante dice qué completar y a qué pantalla ir a completarlo:
// el front avisa con una alerta y el anfitrión navega desde ahí.
// Se comparte entre el alta (modal) y el listado de reservas (aviso previo).

// Credenciales que hacen usable un método de pago: sin al menos una de ellas el
// huésped no tiene a dónde transferir ni qué dato usar. Un método "encendido"
// pero vacío no cuenta como configurado.
const PAYMENT_CREDENTIALS = [
  "alias",
  "cvu",
  "cbu",
  "holder",
  "bank",
  "email",
  "paypalMe",
  "publicKey",
  "accessToken",
  "secretKey",
  "clientId",
  "publishableKey",
];

const hasCredentials = (method) => {
  const keys = method?.keys || {};
  return PAYMENT_CREDENTIALS.some((field) => String(keys[field] ?? "").trim() !== "");
};

export function getBookingRequirements(lodging, payments) {
  const list = [];

  // Un método cuenta como habilitado si no está explícitamente apagado (estados
  // guardados por versiones viejas podían no traer `enabled`) Y tiene datos con
  // los que cobrar: si no, el huésped ve el método pero no puede pagar.
  const hasEnabledMethod = (payments?.methods || []).some(
    (method) => method && method.enabled !== false && hasCredentials(method),
  );

  if (!hasEnabledMethod) {
    list.push({
      key: "payments",
      label: "al menos 1 método de pago habilitado y con datos",
      to: "/panel/pagos",
      page: "Métodos de pago",
    });
  }

  if (!lodging?.logo) {
    list.push({ key: "logo", label: "el logo del alojamiento", to: "/panel/perfil", page: "Perfil" });
  }

  // El huésped ve las fotos en su enlace de llegada: sin al menos una, el
  // alojamiento no está presentable para recibir una reserva.
  const photos = Array.isArray(lodging?.images) ? lodging.images.filter(Boolean) : [];
  if (!photos.length) {
    list.push({
      key: "images",
      label: "al menos 1 foto del alojamiento",
      to: "/panel/alojamiento",
      page: "Alojamiento",
    });
  }

  if (!(lodging?.rooms > 0) || lodging?.bathrooms == null || !(lodging?.capacity > 0)) {
    list.push({
      key: "rooms",
      label: "habitaciones, baños y capacidad",
      to: "/panel/alojamiento",
      page: "Alojamiento",
    });
  }

  if (!String(lodging?.phone || "").trim()) {
    list.push({ key: "phone", label: "el WhatsApp del alojamiento", to: "/panel/perfil", page: "Perfil" });
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(lodging?.email || "").trim())) {
    list.push({ key: "email", label: "un correo de contacto válido para el alojamiento", to: "/panel/alojamiento", page: "Alojamiento" });
  }

  const bank = payments?.bankDetails || {};
  const hasBankDetails = [bank.holder, bank.alias, bank.cbu].some((value) => String(value ?? "").trim() !== "");
  if (!hasEnabledMethod && !hasBankDetails) {
    list.push({
      key: "bank",
      label: "los datos para transferencia (titular, alias o CBU)",
      to: "/panel/pagos",
      page: "Métodos de pago",
    });
  }

  return list;
}

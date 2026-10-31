// Requisitos previos para crear una reserva (entregable C).
// Cada requisito faltante dice qué completar y a qué pantalla ir a completarlo:
// el front avisa con una alerta y el anfitrión navega desde ahí.
// Se comparte entre el alta (modal) y el listado de reservas (aviso previo).
export function getBookingRequirements(lodging, payments) {
  const list = [];

  // Un método cuenta como habilitado salvo que esté explícitamente apagado:
  // estados guardados por versiones viejas podían no traer `enabled` y la
  // alerta bloqueaba la reserva aunque el anfitrión tuviera métodos cargados.
  const hasEnabledMethod = (payments?.methods || []).some((method) => method && method.enabled !== false);

  if (!hasEnabledMethod) {
    list.push({
      key: "payments",
      label: "al menos 1 método de pago habilitado",
      to: "/panel/pagos",
      page: "Métodos de pago",
    });
  }

  if (!lodging?.logo) {
    list.push({ key: "logo", label: "el logo del alojamiento", to: "/panel/perfil", page: "Perfil" });
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

  const bank = payments?.bankDetails || {};
  if (!hasEnabledMethod && !bank.holder && !bank.alias && !bank.cbu) {
    list.push({
      key: "bank",
      label: "los datos para transferencia (titular, alias o CBU)",
      to: "/panel/pagos",
      page: "Métodos de pago",
    });
  }

  return list;
}

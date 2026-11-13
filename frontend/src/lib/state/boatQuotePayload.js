// Construye el objeto JSON de una solicitud de cotización de transporte en lancha.
// Estructura PROVISIONAL documentada en docs/boat-quote-contract.md: cuando la API
// destino confirme su contrato, solo hay que ajustar este mapeo.
//
// Recibe el alojamiento y la reserva por parámetro para no depender del store.
export function buildBoatQuotePayload(lodging, booking, quote) {
  return {
    schema: "hostflow.boat-quote.v1",
    createdAt: new Date().toISOString(),
    lodging: {
      id: lodging?.id || "",
      name: lodging?.name || "",
      location: lodging?.location || "",
      phone: lodging?.phone || "",
    },
    booking: booking
      ? {
          id: booking.id,
          checkIn: booking.checkIn,
          checkOut: booking.checkOut,
          guests: booking.guests,
        }
      : null,
    guest: {
      name: quote.guestName || booking?.guestName || "",
      email: quote.guestEmail || booking?.guestEmail || "",
      phone: quote.guestPhone || "",
    },
    trip: {
      service: "lancha_remis",
      pickupPlace: quote.pickupPlace || "",
      pickupDate: quote.pickupDate || booking?.checkIn || "",
      pickupTime: quote.pickupTime || "",
      dropoffPlace: quote.dropoffPlace || lodging?.location || "",
      passengers: Number(quote.passengers) || 1,
      baggage: Number(quote.baggage) || 0,
      notes: quote.notes || "",
    },
  };
}

// Onboarding del huésped (B2C): proyección + fallback local.
import { actions, store } from "../../state/store"
import { mergeLodging, toBooking } from "./shared"

export const onboardingOps = {
  getOnboarding: {
    // Hidrata el store con el alojamiento, la reserva, los servicios
    // habilitados y las formas de pago activas, de modo que la ficha (y la
    // copia de credenciales de pago) se vea igual en el celular del huésped
    // que en el panel del anfitrión.
    project: (data, tokenOrLodgingId, legacyBookingId) => {
      if (legacyBookingId !== undefined) return data

      const bookingId = String(tokenOrLodgingId)
      actions.updateLodging(mergeLodging({
        id: data?.accommodationId || "public-accommodation",
        name: data?.accommodationName,
        basePrice: data?.pricePerNight,
        currency: data?.currency,
        logoUrl: data?.logoUrl,
        primaryColor: data?.primaryColor,
        accentColor: data?.accentColor,
        tertiaryColor: data?.tertiaryColor,
        backgroundPattern: data?.backgroundPattern,
        welcomeMessage: data?.welcomeMessage,
      }))
      actions.updateLodging({
        logo: data?.logoUrl || null,
        images: [],
        galleryImages: [],
        phone: "",
        location: "",
      })
      const booking = toBooking({
        id: bookingId,
        guestFullName: "",
        checkIn: data?.checkIn,
        checkOut: data?.checkOut,
        guestCount: data?.guestCount,
        totalAmount: data?.totalAmount,
        currency: data?.currency,
        status: "PENDING",
        paymentStatus: "PENDING",
        confirmationCode: data?.confirmationCode,
        confirmationToken: bookingId,
      })
      actions.setBookings([booking])
      if (Array.isArray(data?.additionalServices)) {
        actions.setServices(data.additionalServices.map((service) => ({
          id: String(service.id),
          rawId: service.id,
          name: service.name,
          description: service.description || "",
          price: Number(service.basePrice || 0),
          priceType: service.priceType,
          pricing: service.priceType === "PER_DAY" ? "PER_NIGHT" : "FIXED",
          active: true,
          enabled: true,
        })))
      }
      if (Array.isArray(data?.paymentMethods)) {
        const methods = data.paymentMethods.map((method) => ({
          id: String(method.id),
          label: method.displayName || "Método de pago",
          type: method.type === "BANK_TRANSFER" ? "TRANSFER" : method.type === "CARD" ? "CARD" : "WALLET",
          enabled: true,
          keys: {
            holder: method.accountHolder,
            alias: method.alias,
            cbu: method.cbuCvu,
            bank: method.bankName,
            email: method.walletEmail,
          },
          notes: method.instructions,
        }))
        const bank = data.paymentMethods.find((method) => method.type === "BANK_TRANSFER")
        actions.setPayments({
          methods,
          bankDetails: bank ? {
            holder: bank.accountHolder,
            alias: bank.alias,
            cbu: bank.cbuCvu,
            bank: bank.bankName,
            notes: bank.instructions,
          } : { id: null, holder: "", alias: "", cbu: "", bank: "", accountType: "", accountNumber: "", notes: "" },
        })
      }
      return data
    },
    local: () => null,
  },

  submitOnboarding: {
    // El enlace público trae lodgingId y bookingId; el panel llama solo con el
    // bookingId, así que el alojamiento sale del store.
    send: (id, data) => {
      const booking = store.get().bookings.find((item) => String(item.id) === String(id))
      return [booking?.confirmationToken || id, data]
    },
    project: (response, id, data) => {
      actions.updateBooking(id, {
        guestName: data.guestName,
        guestEmail: data.guestEmail,
        guestPhone: data.guestPhone,
        selectedServices: Array.isArray(data.selectedServices) ? data.selectedServices : [],
        status: response?.reservationStatus || "CONFIRMED",
        paymentStatus: response?.paymentStatus || "PENDING",
        total: Number(response?.totalAmount ?? store.get().bookings.find((booking) => String(booking.id) === String(id))?.total ?? 0),
        onboarding: { ...data, completedAt: new Date().toISOString() },
      })
      return data
    },
    local: (id, data) => {
      actions.submitOnboarding(id, data)
      return data
    },
  },

  createBoatQuoteRequest: {
    project: (created, id) => {
      actions.updateBooking(id, { boatQuote: created })
      return created
    },
    local: (id, quote) => actions.createBoatQuoteRequest(id, quote),
  },

  declineBoatQuote: {
    project: (_response, id) => actions.declineBoatQuote(id),
    local: (id) => actions.declineBoatQuote(id),
  },
}

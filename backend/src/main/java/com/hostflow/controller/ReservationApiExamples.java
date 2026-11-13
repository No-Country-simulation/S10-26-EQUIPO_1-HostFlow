package com.hostflow.controller;

/**
 * Ejemplos OpenAPI del módulo de reservas.
 * Deben ser constantes de compilación (solo literales; sin métodos).
 */
public final class ReservationApiExamples {

    private ReservationApiExamples() {}

    public static final String REQ_CREATE = """
            {
              "guestFirstName": "Juan Carlos",
              "guestLastName": "Pérez",
              "guestEmail": "juan.perez@email.com",
              "guestPhone": "+54 11 5555 5555",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "guestCount": 2,
              "specialRequests": "Necesito cuna para bebé"
            }
            """;

    public static final String RES_PENDING = """
            {
              "id": 12,
              "confirmationCode": "HF-A1B2C3",
              "accommodationId": 1,
              "accommodationName": "Cabañas del Lago",
              "guestId": null,
              "guestFullName": "Juan Carlos Pérez",
              "guestEmail": "juan.perez@email.com",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "pricePerNight": 45000.00,
              "subtotal": 135000.00,
              "taxes": 0,
              "totalAmount": 135000.00,
              "status": "PENDING",
              "paymentStatus": "PENDING",
              "paidAt": null,
              "guestCount": 2,
              "specialRequests": "Necesito cuna para bebé",
              "confirmationLink": {
                "id": 1,
                "token": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "confirmationUrl": "https://app.hostflow.com/reservations/confirm?token=3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "createdAt": "2026-11-10T20:00:00",
                "expiresAt": "2026-11-11T20:00:00",
                "used": false,
                "usedAt": null,
                "reactivationCount": 0,
                "isValid": true,
                "isExpired": false
              },
              "createdAt": "2026-11-10T20:00:00",
              "updatedAt": "2026-11-10T20:00:00"
            }
            """;

    public static final String RES_CONFIRMED = """
            {
              "id": 12,
              "confirmationCode": "HF-A1B2C3",
              "accommodationId": 1,
              "accommodationName": "Cabañas del Lago",
              "guestId": 7,
              "guestFullName": "Juan Carlos Pérez",
              "guestEmail": "juan.perez@email.com",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "pricePerNight": 45000.00,
              "subtotal": 135000.00,
              "taxes": 0,
              "totalAmount": 150000.00,
              "status": "CONFIRMED",
              "paymentStatus": "PENDING",
              "paidAt": null,
              "guestCount": 2,
              "specialRequests": "Necesito cuna para bebé",
              "confirmationLink": null,
              "createdAt": "2026-11-10T20:00:00",
              "updatedAt": "2026-11-10T21:00:00"
            }
            """;

    public static final String RES_PAID = """
            {
              "id": 12,
              "confirmationCode": "HF-A1B2C3",
              "accommodationId": 1,
              "accommodationName": "Cabañas del Lago",
              "guestId": 7,
              "guestFullName": "Juan Carlos Pérez",
              "guestEmail": "juan.perez@email.com",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "pricePerNight": 45000.00,
              "subtotal": 135000.00,
              "taxes": 0,
              "totalAmount": 150000.00,
              "status": "CONFIRMED",
              "paymentStatus": "PAID",
              "paidAt": "2026-11-11T10:30:00",
              "guestCount": 2,
              "specialRequests": "Necesito cuna para bebé",
              "confirmationLink": null,
              "createdAt": "2026-11-10T20:00:00",
              "updatedAt": "2026-11-11T10:30:00"
            }
            """;

    public static final String RES_COMPLETED = """
            {
              "id": 12,
              "confirmationCode": "HF-A1B2C3",
              "accommodationId": 1,
              "accommodationName": "Cabañas del Lago",
              "guestId": 7,
              "guestFullName": "Juan Carlos Pérez",
              "guestEmail": "juan.perez@email.com",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "pricePerNight": 45000.00,
              "subtotal": 135000.00,
              "taxes": 0,
              "totalAmount": 150000.00,
              "status": "COMPLETED",
              "paymentStatus": "PAID",
              "paidAt": "2026-11-11T10:30:00",
              "guestCount": 2,
              "specialRequests": "Necesito cuna para bebé",
              "confirmationLink": null,
              "createdAt": "2026-11-10T20:00:00",
              "updatedAt": "2026-12-19T09:00:00"
            }
            """;


    public static final String RES_LINK = """
            {
              "id": 2,
              "token": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
              "confirmationUrl": "https://app.hostflow.com/reservations/confirm?token=7c9e6679-7425-40de-944b-e07fc1f90ae7",
              "createdAt": "2026-11-10T22:00:00",
              "expiresAt": "2026-11-11T22:00:00",
              "used": false,
              "usedAt": null,
              "reactivationCount": 0,
              "isValid": true,
              "isExpired": false
            }
            """;

    public static final String RES_CANCELLED = """
            {
              "id": 12,
              "confirmationCode": "HF-A1B2C3",
              "accommodationId": 1,
              "accommodationName": "Cabañas del Lago",
              "guestId": null,
              "guestFullName": "Juan Carlos Pérez",
              "guestEmail": "juan.perez@email.com",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "pricePerNight": 45000.00,
              "subtotal": 135000.00,
              "taxes": 0,
              "totalAmount": 135000.00,
              "status": "CANCELLED",
              "paymentStatus": "PENDING",
              "paidAt": null,
              "guestCount": 2,
              "specialRequests": "Necesito cuna para bebé",
              "confirmationLink": null,
              "createdAt": "2026-11-10T20:00:00",
              "updatedAt": "2026-11-10T22:00:00"
            }
            """;

    /** Lista con un elemento PENDING (literal completo; no concatenar en runtime). */
    public static final String RES_LIST = """
            [{
              "id": 12,
              "confirmationCode": "HF-A1B2C3",
              "accommodationId": 1,
              "accommodationName": "Cabañas del Lago",
              "guestId": null,
              "guestFullName": "Juan Carlos Pérez",
              "guestEmail": "juan.perez@email.com",
              "checkIn": "2026-12-15",
              "checkOut": "2026-12-18",
              "nights": 3,
              "pricePerNight": 45000.00,
              "subtotal": 135000.00,
              "taxes": 0,
              "totalAmount": 135000.00,
              "status": "PENDING",
              "paymentStatus": "PENDING",
              "paidAt": null,
              "guestCount": 2,
              "specialRequests": "Necesito cuna para bebé",
              "confirmationLink": {
                "id": 1,
                "token": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "confirmationUrl": "https://app.hostflow.com/reservations/confirm?token=3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "createdAt": "2026-11-10T20:00:00",
                "expiresAt": "2026-11-11T20:00:00",
                "used": false,
                "usedAt": null,
                "reactivationCount": 0,
                "isValid": true,
                "isExpired": false
              },
              "createdAt": "2026-11-10T20:00:00",
              "updatedAt": "2026-11-10T20:00:00"
            }]
            """;

    public static final String ERR_401 = """
            {"timestamp":"2026-11-10T20:00:00","status":401,"error":"Unauthorized","message":"Debes autenticarte para acceder a este recurso.","errorCode":"UNAUTHORIZED","path":"/owners/reservations"}
            """;

    public static final String ERR_403 = """
            {"timestamp":"2026-11-10T20:00:00","status":403,"error":"Forbidden","message":"No tienes permisos para acceder a este recurso.","errorCode":"FORBIDDEN","path":"/owners/reservations"}
            """;

    public static final String ERR_400_DATES = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"Las fechas seleccionadas no están disponibles.","errorCode":"BUSINESS_ERROR"}
            """;

    public static final String ERR_400_VALIDATION = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"Error de validación en los datos enviados","errorCode":"VALIDATION_ERROR"}
            """;

    public static final String ERR_400_CANCEL = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"No se puede cancelar una reserva que ya ha sido completada.","errorCode":"BUSINESS_ERROR"}
            """;

    public static final String ERR_400_REACTIVATE = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"Solo se puede renovar el enlace de reservas en estado PENDING.","errorCode":"BUSINESS_ERROR"}
            """;

    public static final String ERR_400_MARK_PAID = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"Solo se puede registrar el pago de una reserva CONFIRMED. Estado actual: PENDING","errorCode":"BUSINESS_ERROR"}
            """;

    public static final String ERR_400_COMPLETE_PAID = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"Solo se pueden completar reservas con pago registrado (PAID). Registra el pago antes de completar la estadía.","errorCode":"BUSINESS_ERROR"}
            """;

    public static final String ERR_400_COMPLETE_DATE = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"La reserva solo puede completarse a partir del día siguiente al check-out. Check-out: 2026-12-18","errorCode":"BUSINESS_ERROR"}
            """;

    public static final String ERR_400_DATES_INVALID = """
            {"timestamp":"2026-11-10T20:00:00","status":400,"error":"Bad Request","message":"La fecha de check-out debe ser posterior a la de check-in.","errorCode":"BUSINESS_ERROR"}
            """;

    public static final String ERR_404_RESERVATION = """
            {"timestamp":"2026-11-10T20:00:00","status":404,"error":"Not Found","message":"No se encontró la reserva con ID: 12","errorCode":"RESOURCE_NOT_FOUND"}
            """;

    public static final String ERR_404_ACCOMMODATION = """
            {"timestamp":"2026-11-10T20:00:00","status":404,"error":"Not Found","message":"No tienes un alojamiento registrado. Registra los datos de tu alojamiento primero.","errorCode":"RESOURCE_NOT_FOUND"}
            """;
}

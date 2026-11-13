# HostFlow — parche puntual de pago y confirmación de reserva

Copiar los archivos incluidos conservando sus rutas relativas dentro del proyecto `backend/`.

## Endpoint nuevo
`POST /owners/reservations/{reservationId}/mark-paid`

Requiere autenticación con rol OWNER. El propietario debe verificar el comprobante recibido por WhatsApp antes de invocarlo.

## Comportamiento
- Solo permite registrar el pago de una reserva `CONFIRMED` cuyo huésped completó el onboarding.
- Cambia `paymentStatus` a `PAID`, registra `paidAt` y `paymentMarkedBy`.
- Bloquea la reserva durante la transición para evitar doble procesamiento concurrente.
- Si ya está `PAID`, la operación es idempotente y no vuelve a enviar correos.
- Los correos se procesan después del commit de base de datos.
- Envía confirmación al huésped y notificación al propietario.
- `ReservationResponse` incluye `paymentStatus` y `paidAt` para que el frontend pueda mostrar el estado.
- Las plantillas reutilizadas muestran los datos del huésped, fechas, importes, preferencias y alergias; el correo del propietario también muestra el documento.
- El diseño usa logo, `primaryColor` y `accentColor` del alojamiento.

## Importante
- No incluye cambios de Flyway ni migraciones.
- El correo final no se envía al completar el onboarding; solo se dispara al registrar `PAID`.
- El botón de los correos usa `#` como destino temporal. Conectar una URL real cuando esté definida la ruta de detalle de reserva del frontend.
- No se pudo ejecutar Maven en este entorno: `mvn` no está instalado y el wrapper no pudo descargar Maven por falta de acceso al repositorio. Compilar y probar en el entorno local antes de integrar.

## Prueba manual sugerida
1. Completar onboarding y verificar `paymentStatus = PENDING`.
2. Confirmar que no llega el correo final de confirmación.
3. Como propietario autenticado, llamar `POST /owners/reservations/{id}/mark-paid`.
4. Verificar `paymentStatus = PAID`, `paidAt` y `paymentMarkedBy`.
5. Verificar ambos correos.
6. Repetir el endpoint y comprobar que no se envían correos duplicados.
7. Probar con el ID de una reserva de otro propietario y verificar que responde como no encontrada.

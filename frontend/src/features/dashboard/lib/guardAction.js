// Las acciones de estado (confirmar / cancelar reserva, marcar como pagada,
// renovar el enlace de llegada) actualizan el store de inmediato para que la
// interfaz responda sin esperar al servidor.
//
// Si el backend devuelve un error REAL (400, 409, 500…), la capa de servicios
// lo propaga; acá se deja constancia en consola sin romper la pantalla, ya que
// estos controles no llevan mensaje de error propio. Los endpoints aún no
// implementados no llegan acá: httpApi los resuelve contra el store local.
export function guardAction(label, promise) {
  return Promise.resolve(promise).catch((err) => console.warn(`${label}:`, err));
}

package com.hostflow.service;

import com.hostflow.model.LoginAttempt;
import com.hostflow.model.Owner;
import com.hostflow.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio encargado de gestionar los intentos de inicio de sesión
 * y el bloqueo automático de cuentas por seguridad.
 *
 * <p>Este servicio se ejecuta en transacciones independientes
 * ({@code REQUIRES_NEW}) para garantizar que los registros de auditoría
 * se persistan en la base de datos incluso cuando el método de login
 * lance una excepción y revierta su propia transacción.</p>
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Registrar cada intento de login (exitoso o fallido) en la tabla {@code login_attempts}.</li>
 *   <li>Contar los intentos fallidos recientes dentro de una ventana de tiempo configurable.</li>
 *   <li>Bloquear automáticamente la cuenta tras alcanzar el máximo de intentos fallidos.</li>
 *   <li>Generar mensajes de error descriptivos con información de intentos restantes.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    /**
     * Repositorio para operaciones de persistencia de la entidad {@link LoginAttempt}.
     */
    private final LoginAttemptRepository loginAttemptRepository;

    /**
     * Repositorio para operaciones de persistencia de la entidad {@link Owner}.
     * Se utiliza para actualizar el campo {@code blockedUntil} cuando se bloquea una cuenta.
     */
    private final com.hostflow.repository.OwnerRepository ownerRepository;

    // =========================================================================
    // CONFIGURACIÓN DE SEGURIDAD (Inyectada desde application.properties)
    // =========================================================================

    /**
     * Número máximo de intentos fallidos de login antes de bloquear la cuenta.
     * Configurado por defecto en 5 intentos.
     */
    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    /**
     * Duración del bloqueo de cuenta en minutos.
     * Configurado por defecto en 60 minutos (1 hora).
     */
    @Value("${app.security.block-duration-minutes:60}")
    private int blockDurationMinutes;

    /**
     * Ventana de tiempo en minutos para contar los intentos fallidos.
     * Los intentos fallidos más antiguos que este período no se cuentan.
     * Configurado por defecto en 60 minutos.
     */
    @Value("${app.security.failed-attempts-window-minutes:60}")
    private int failedAttemptsWindowMinutes;

    // =========================================================================
    // REGISTRO DE INTENTOS DE LOGIN (Transacción Independiente)
    // =========================================================================

    /**
     * Registra un intento de login en la tabla de auditoría {@code login_attempts}
     * en una transacción independiente.
     *
     * <p>La anotación {@code REQUIRES_NEW} garantiza que este método se ejecute
     * en su propia transacción, separada de la transacción del método que lo invoca.
     * Esto es crucial para que el registro se persista incluso si el método
     * llamante lanza una excepción y hace rollback.</p>
     *
     * @param owner        la entidad del usuario (puede ser {@code null} si el email no existe)
     * @param rolesList    lista de roles del usuario
     * @param success      {@code true} si el login fue exitoso, {@code false} si falló
     * @param blocked      {@code true} si este intento causó el bloqueo de la cuenta
     * @param blockedUntil fecha y hora hasta cuando está bloqueada la cuenta ({@code null} si no está bloqueada)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerLoginAttempt(Owner owner, List<String> rolesList,
                                     boolean success, boolean blocked, LocalDateTime blockedUntil) {
        String email = owner != null ? owner.getEmail() : "unknown";
        String fullName = owner != null ? owner.getFullName() : "Desconocido";
        Long ownerId = owner != null ? owner.getId() : null;
        String role = (rolesList != null && !rolesList.isEmpty()) ? rolesList.get(0) : "UNKNOWN";

        LoginAttempt attempt = LoginAttempt.builder()
                .ownerId(ownerId)
                .email(email)
                .fullName(fullName)
                .role(role)
                .success(success)
                .blocked(blocked)
                .blockedUntil(blockedUntil)
                .build();

        loginAttemptRepository.save(attempt);

        log.debug(" Registro de intento de login: email={}, success={}, blocked={}", email, success, blocked);
    }

    // =========================================================================
    // MANEJO DE INTENTOS FALLIDOS (Transacción Independiente)
    // =========================================================================

    /**
     * Maneja un intento de login fallido con transacción independiente.
     *
     * <p>Este método se ejecuta en una nueva transacción ({@code REQUIRES_NEW})
     * para garantizar que el registro del intento fallido se persista en la base
     * de datos incluso cuando el método {@code login()} lance una excepción y
     * revierta su propia transacción.</p>
     *
     * <p>Flujo del proceso:</p>
     * <ol>
     *   <li>Cuenta los intentos fallidos recientes dentro de la ventana de tiempo configurada.</li>
     *   <li>Calcula el número total de intentos (incluyendo el actual).</li>
     *   <li>Si alcanza el máximo de intentos fallidos, bloquea la cuenta por el tiempo configurado.</li>
     *   <li>Registra el intento fallido en la base de datos.</li>
     *   <li>Retorna un mensaje de error descriptivo con información de intentos restantes o bloqueo.</li>
     * </ol>
     *
     * @param email el correo electrónico utilizado en el intento fallido
     * @param owner la entidad del usuario (puede ser {@code null} si el email no existe)
     * @return mensaje de error descriptivo con información de intentos restantes o bloqueo
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String handleFailedLoginAttempt(String email, Owner owner) {
        // 1. Preparar datos para el registro
        List<String> rolesList = owner != null ? extractRolesAsStringList(owner) : List.of();

        // 2. Calcular la ventana de tiempo para contar intentos fallidos
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(failedAttemptsWindowMinutes);

        // 3. Contar intentos fallidos recientes (ANTES de insertar el nuevo)
        long failedAttemptsCount = loginAttemptRepository.countFailedAttemptsSince(email, windowStart);

        // 4. Calcular el número total de intentos (incluyendo el actual)
        long totalAttempts = failedAttemptsCount + 1;

        // 5. Verificar si se alcanzó el máximo de intentos fallidos
        boolean shouldBlock = totalAttempts >= maxFailedAttempts;

        // 6. Calcular fecha de bloqueo si es necesario
        LocalDateTime blockedUntil = null;
        if (shouldBlock && owner != null) {
            blockedUntil = LocalDateTime.now().plusMinutes(blockDurationMinutes);
            owner.setBlockedUntil(blockedUntil);
            ownerRepository.save(owner);
            log.warn("🚨 CUENTA BLOQUEADA: {} - Bloqueada hasta: {} ({} intentos fallidos)",
                    email, blockedUntil, totalAttempts);
        }

        // 7. Registrar el intento fallido en la base de datos (en esta transacción independiente)
        registerLoginAttemptInternal(owner, rolesList, false, shouldBlock, blockedUntil);

        // 8. Construir el mensaje de error con información de intentos restantes
        String errorMessage;
        if (shouldBlock) {
            errorMessage = String.format(
                    "Credenciales incorrectas. Su cuenta ha sido bloqueada por seguridad durante %d minutos " +
                            "tras %d intentos fallidos. Si olvidó su contraseña, use la opción 'Recuperar contraseña'.",
                    blockDurationMinutes, totalAttempts
            );
            log.warn("⚠️ Intento fallido número {} para email: {} - Cuenta bloqueada por {} minutos",
                    totalAttempts, email, blockDurationMinutes);
        } else {
            int remainingAttempts = maxFailedAttempts - (int) totalAttempts;
            errorMessage = String.format(
                    "Credenciales incorrectas. Le quedan %d intento(s) antes de que su cuenta sea bloqueada por %d minutos.",
                    remainingAttempts, blockDurationMinutes
            );
            log.warn("⚠️ Intento fallido número {} para email: {} - Quedan {} intento(s) antes del bloqueo",
                    totalAttempts, email, remainingAttempts);
        }

        return errorMessage;
    }

    /**
     * Método interno para registrar un intento de login dentro de la misma transacción.
     * Se usa cuando ya estamos dentro de una transacción {@code REQUIRES_NEW}.
     *
     * @param owner        la entidad del usuario
     * @param rolesList    lista de roles del usuario
     * @param success      {@code true} si el login fue exitoso, {@code false} si falló
     * @param blocked      {@code true} si este intento causó el bloqueo de la cuenta
     * @param blockedUntil fecha y hora hasta cuando está bloqueada la cuenta
     */
    private void registerLoginAttemptInternal(Owner owner, List<String> rolesList,
                                              boolean success, boolean blocked, LocalDateTime blockedUntil) {
        String email = owner != null ? owner.getEmail() : "unknown";
        String fullName = owner != null ? owner.getFullName() : "Desconocido";
        Long ownerId = owner != null ? owner.getId() : null;
        String role = (rolesList != null && !rolesList.isEmpty()) ? rolesList.get(0) : "UNKNOWN";

        LoginAttempt attempt = LoginAttempt.builder()
                .ownerId(ownerId)
                .email(email)
                .fullName(fullName)
                .role(role)
                .success(success)
                .blocked(blocked)
                .blockedUntil(blockedUntil)
                .build();

        loginAttemptRepository.save(attempt);

        log.debug(" Registro interno de intento de login: email={}, success={}, blocked={}", email, success, blocked);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Extrae los roles de una entidad {@link Owner} y los convierte en una lista de Strings.
     *
     * @param owner la entidad del dueño
     * @return lista de nombres de roles en formato String (ej. ["ROLE_OWNER"])
     */
    private List<String> extractRolesAsStringList(Owner owner) {
        if (owner == null || owner.getRoles() == null) {
            return List.of();
        }
        return owner.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(java.util.stream.Collectors.toList());
    }
}
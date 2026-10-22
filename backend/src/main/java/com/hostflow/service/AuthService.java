package com.hostflow.service;

import com.hostflow.config.InactivityConfig;
import com.hostflow.dto.request.LoginRequest;
import com.hostflow.dto.request.OwnerRegisterRequest;
import com.hostflow.dto.response.AuthResponse;
import com.hostflow.enums.RoleType;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.EmailVerificationToken;
import com.hostflow.model.Owner;
import com.hostflow.model.Role;
import com.hostflow.repository.EmailVerificationTokenRepository;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio central de autenticación, registro y gestión de identidad para HostFlow.
 *
 * <p>Orquesta el flujo completo de seguridad del usuario, coordinando la interacción
 * entre repositorios de datos, codificación de contraseñas, generación de tokens JWT,
 * envío de correos electrónicos y el gestor de autenticación de Spring Security.</p>
 *
 * <p><strong>Responsabilidades principales:</strong></p>
 * <ul>
 *   <li>Registro de nuevos usuarios con generación automática de token de verificación de email.</li>
 *   <li>Restricción de acceso: el usuario no puede iniciar sesión hasta verificar su correo.</li>
 *   <li>Validación de estado de cuenta: bloqueo por intentos fallidos o deshabilitación administrativa.</li>
 *   <li>Autenticación segura con generación de tokens JWT y metadatos de sesión.</li>
 *   <li>Auditoría completa: registro de todos los intentos de login en la base de datos.</li>
 * </ul>
 *
 * <p><strong>Políticas de Seguridad Implementadas:</strong></p>
 * <ul>
 *   <li><strong>Verificación de Email:</strong> Token UUID único, válido por 1 hora, de un solo uso.</li>
 *   <li><strong>Bloqueo por Fuerza Bruta:</strong> Tras 5 intentos fallidos, bloqueo de 1 hora.
 *       Se informa al usuario de los intentos restantes antes del bloqueo.</li>
 *   <li><strong>Deshabilitación Administrativa:</strong> Si un admin desactiva la cuenta,
 *       se registra la fecha y se muestra un mensaje específico al intentar hacer login.</li>
 *   <li><strong>Prevención de Enumeración:</strong> Mensajes genéricos para emails no registrados.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 5.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS (Inmutables gracias a @RequiredArgsConstructor)
    // =========================================================================

    private final OwnerRepository ownerRepository;
    private final RoleRepository roleRepository;

    /** 🆕 Repositorio para gestionar los tokens de verificación de correo electrónico. */
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final InactivityConfig inactivityConfig;

    /**
     * 🆕 Servicio externo para manejar intentos fallidos en transacción independiente
     * ({@code REQUIRES_NEW}), garantizando que el registro de auditoría no se revierta
     * si el método de login lanza una excepción.
     */
    private final LoginAttemptService loginAttemptService;

    /** 🆕 Servicio para el envío de correos electrónicos (verificación, recuperación, etc.). */
    private final EmailService emailService;

    // =========================================================================
    // CONFIGURACIÓN DE SEGURIDAD (Inyectada desde application.properties)
    // =========================================================================

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.block-duration-minutes:60}")
    private int blockDurationMinutes;

    @Value("${app.security.failed-attempts-window-minutes:60}")
    private int failedAttemptsWindowMinutes;

    /** 🆕 Tiempo de validez del enlace de verificación de correo en minutos. */
    @Value("${app.email-verification.expiration-minutes:60}")
    private int emailVerificationExpirationMinutes;

    /** 🆕 URL base del backend para la verificación directa del token. */
    @Value("${app.email-verification.backend-url:http://localhost:8080/api/v1/auth/verify-email}")
    private String backendVerificationUrl;

    // =========================================================================
    // REGISTRO DE USUARIO CON VERIFICACIÓN DE EMAIL
    // =========================================================================

    /**
     * Registra un nuevo dueño de alojamiento en el sistema.
     *
     * <p>Flujo del proceso:</p>
     * <ol>
     *   <li>Valida aceptación de términos y unicidad del correo electrónico.</li>
     *   <li>Obtiene o crea el rol {@code ROLE_OWNER} de forma idempotente.</li>
     *   <li>Encripta la contraseña con BCrypt y crea la entidad con {@code emailVerified = false}.</li>
     *   <li>Persiste el nuevo dueño en la base de datos.</li>
     *   <li>Genera un token UUID único de verificación válido por 1 hora.</li>
     *   <li>Envía el correo electrónico con el enlace de verificación al usuario.</li>
     *   <li>Registra el intento de "login" exitoso tras el registro en la auditoría.</li>
     *   <li>Retorna la respuesta de autenticación enriquecida indicando que se envió el email.</li>
     * </ol>
     *
     * @param request los datos de registro del dueño (validados por Bean Validation)
     * @return la respuesta de autenticación con el token JWT y metadatos de sesión
     * @throws BusinessException si el correo ya está registrado o no se aceptaron los términos
     */
    @Transactional
    public AuthResponse registerOwner(OwnerRegisterRequest request) {

        // 1. Validación de reglas de negocio críticas
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new BusinessException("Debe aceptar los términos y la política de privacidad para registrarse");
        }

        if (ownerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Ya existe una cuenta registrada con el correo: " + request.getEmail());
        }

        // 2. Obtención o creación del rol por defecto (Idempotente)
        Role ownerRole = roleRepository.findByRoleName(RoleType.ROLE_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_OWNER)));

        // 3. Construcción y persistencia de la entidad Owner
        // ⚠️ emailVerified = false: el usuario debe verificar su correo antes de poder usar la cuenta
        Owner owner = Owner.builder()
                .fullName(request.getFullName())
                .accommodationName(request.getAccommodationName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .termsAccepted(request.getTermsAccepted())
                .enabled(true)
                .emailVerified(false) // 🆕 Cuenta nueva, correo NO verificado
                .blockedUntil(null)   // Cuenta nueva, sin bloqueo
                .roles(Set.of(ownerRole))
                .build();

        Owner savedOwner = ownerRepository.save(owner);
        List<String> rolesList = extractRolesAsStringList(savedOwner);

        // 4. Generación y persistencia del token de verificación de correo
        String verificationToken = generateEmailVerificationToken(savedOwner);
        String verificationUrl = buildVerificationUrl(verificationToken);

        // 5. Envío del correo electrónico de verificación
        emailService.sendEmailVerificationEmail(
                savedOwner.getEmail(),
                verificationToken,
                verificationUrl,
                emailVerificationExpirationMinutes
        );

        // 6. Generación del token JWT (el frontend puede usarlo, pero el login futuro estará restringido)
        String token = jwtService.generateToken(
                new User(
                        savedOwner.getEmail(),
                        savedOwner.getPassword(),
                        convertToSimpleGrantedAuthorities(savedOwner)
                )
        );

        // 7. Registro de auditoría: Login exitoso tras registro
        loginAttemptService.registerLoginAttempt(savedOwner, rolesList, true, false, null);

        log.info("✅ Usuario registrado exitosamente: {}. Email de verificación enviado.", savedOwner.getEmail());

        // 8. Construcción y retorno de la respuesta enriquecida (true = se envió email de verificación)
        return buildAuthResponse(savedOwner, token, rolesList, true);
    }

    // =========================================================================
    // VERIFICACIÓN DE CORREO ELECTRÓNICO
    // =========================================================================

    /**
     * Verifica el correo electrónico de un usuario utilizando el token de verificación.
     *
     * <p>Flujo del proceso:</p>
     * <ol>
     *   <li>Busca el token por su código en la base de datos.</li>
     *   <li>Valida que el token no haya expirado (máximo 1 hora).</li>
     *   <li>Valida que el token no haya sido utilizado previamente (un solo uso).</li>
     *   <li>Busca al usuario por email y actualiza {@code emailVerified} a {@code true}.</li>
     *   <li>Marca el token como utilizado e invalida otros tokens pendientes para ese email.</li>
     * </ol>
     *
     * @param token el token UUID de verificación recibido por email
     * @return mensaje de confirmación de la verificación exitosa
     * @throws BusinessException si el token es inválido, expirado o ya usado
     */
    @Transactional
    public String verifyEmail(String token) {
        // 1. Buscar el token por código
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("El enlace de verificación es inválido o no existe."));

        // 2. Validar que el token no haya expirado
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("El enlace de verificación ha expirado. Solicita un nuevo enlace de verificación.");
        }

        // 3. Validar que el token no haya sido utilizado
        if (Boolean.TRUE.equals(verificationToken.getUsed())) {
            throw new BusinessException("El enlace de verificación ya fue utilizado. Tu cuenta ya está verificada.");
        }

        // 4. Buscar al usuario por email
        String email = verificationToken.getEmail();
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("No se encontró un usuario asociado a este token."));

        // 5. Validar que la cuenta no esté ya verificada (idempotencia)
        if (Boolean.TRUE.equals(owner.getEmailVerified())) {
            verificationToken.setUsed(true);
            emailVerificationTokenRepository.save(verificationToken);
            return "Tu correo electrónico ya fue verificado previamente. Ya puedes iniciar sesión.";
        }

        // 6. Actualizar el campo emailVerified a true
        owner.setEmailVerified(true);
        ownerRepository.save(owner);

        // 7. Marcar el token como utilizado
        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        // 8. Invalidar otros tokens pendientes para este email (limpieza de seguridad)
        var pendingTokens = emailVerificationTokenRepository.findByEmailAndUsedFalse(email);
        pendingTokens.stream()
                .filter(t -> !t.getId().equals(verificationToken.getId()))
                .forEach(t -> {
                    t.setUsed(true);
                    emailVerificationTokenRepository.save(t);
                });

        log.info("✅ Correo electrónico verificado exitosamente para: {}", email);
        return "¡Correo electrónico verificado exitosamente! Ya puedes iniciar sesión con tus credenciales.";
    }

    /**
     * Reenvía el enlace de verificación de correo electrónico.
     *
     * <p>Se utiliza cuando el usuario no recibió el correo original o el enlace expiró.
     * Invalida los tokens anteriores y genera uno nuevo.</p>
     *
     * <p><strong>Nota de seguridad:</strong> Siempre retorna éxito incluso si el email
     * no está registrado, para prevenir ataques de enumeración de usuarios.</p>
     *
     * @param email el correo electrónico del usuario
     * @return mensaje de confirmación (genérico por seguridad)
     */
    @Transactional
    public String resendEmailVerification(String email) {
        Owner owner = ownerRepository.findByEmail(email).orElse(null);

        // Por seguridad, no revelar si el email existe o no
        if (owner == null) {
            log.warn("⚠️ Solicitud de reenvío de verificación para email no registrado: {}", email);
            return "Si el correo está registrado, recibirás un nuevo enlace de verificación.";
        }

        // Si ya está verificado, no tiene sentido reenviar
        if (Boolean.TRUE.equals(owner.getEmailVerified())) {
            return "Tu correo electrónico ya fue verificado. Ya puedes iniciar sesión.";
        }

        // Invalidar tokens anteriores pendientes
        var oldTokens = emailVerificationTokenRepository.findByEmailAndUsedFalse(email);
        if (!oldTokens.isEmpty()) {
            oldTokens.forEach(t -> {
                t.setUsed(true);
                emailVerificationTokenRepository.save(t);
            });
            log.info("🗑️ Tokens de verificación anteriores invalidados para: {}", email);
        }

        // Generar y enviar nuevo token
        String verificationToken = generateEmailVerificationToken(owner);
        String verificationUrl = buildVerificationUrl(verificationToken);

        emailService.sendEmailVerificationResendEmail(
                email, verificationToken, verificationUrl, emailVerificationExpirationMinutes
        );

        log.info("✅ Nuevo enlace de verificación enviado a: {}", email);
        return "Si el correo está registrado, recibirás un nuevo enlace de verificación.";
    }

    // =========================================================================
    // INICIO DE SESIÓN (LOGIN) CON VALIDACIÓN DE ESTADO Y BLOQUEO
    // =========================================================================

    /**
     * Autentica un usuario existente y genera un token JWT.
     *
     * <p>Flujo del proceso con seguridad de bloqueo y verificación:</p>
     * <ol>
     *   <li>Busca al usuario por email en la base de datos.</li>
     *   <li>🆕 Verifica si la cuenta ha sido deshabilitada por el administrador.</li>
     *   <li>Verifica si la cuenta está bloqueada por intentos fallidos (blockedUntil > ahora).</li>
     *   <li>Verifica si el correo electrónico ha sido verificado ({@code emailVerified == true}).</li>
     *   <li>Intenta autenticar las credenciales con el AuthenticationManager.</li>
     *   <li>Si falla: delega al {@link LoginAttemptService} para registrar y evaluar bloqueo.</li>
     *   <li>Si es exitoso: desbloquea la cuenta (si estaba bloqueada), registra el intento
     *       exitoso y genera el token JWT.</li>
     * </ol>
     *
     * @param request las credenciales del usuario (email y contraseña)
     * @return la respuesta de autenticación con el token JWT y metadatos de sesión
     * @throws BusinessException si las credenciales son incorrectas, el usuario no existe,
     *         la cuenta está bloqueada, deshabilitada o el correo no ha sido verificado
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();

        // 1. Buscar al usuario en la base de datos
        Owner owner = ownerRepository.findByEmail(email).orElse(null);

        // 2. 🆕 Verificar si la cuenta ha sido deshabilitada por el administrador
        if (owner != null && !Boolean.TRUE.equals(owner.getEnabled())) {
            List<String> rolesList = extractRolesAsStringList(owner);
            loginAttemptService.registerLoginAttempt(owner, rolesList, false, false, null);

            String disableMessage = "Tu cuenta ha sido deshabilitada por el administrador.";
            if (owner.getDisabledAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
                disableMessage += String.format(" Fecha de deshabilitación: %s.", owner.getDisabledAt().format(formatter));
            }
            disableMessage += " Por favor, contacta con soporte para más información.";

            throw new BusinessException(disableMessage);
        }

        // 3. Verificar si la cuenta está bloqueada por intentos fallidos
        if (owner != null && isAccountBlocked(owner)) {
            LocalDateTime blockedUntil = owner.getBlockedUntil();
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), blockedUntil).toMinutes();

            List<String> rolesList = extractRolesAsStringList(owner);
            loginAttemptService.registerLoginAttempt(owner, rolesList, false, false, blockedUntil);

            throw new BusinessException(
                    String.format("Cuenta bloqueada por seguridad. Intenta nuevamente en %d minutos. " +
                            "Si olvidaste tu contraseña, usa la opción 'Recuperar contraseña'.", minutesRemaining)
            );
        }

        // 4. Verificar si el correo electrónico ha sido verificado
        if (owner != null && !Boolean.TRUE.equals(owner.getEmailVerified())) {
            List<String> rolesList = extractRolesAsStringList(owner);
            loginAttemptService.registerLoginAttempt(owner, rolesList, false, false, null);

            throw new BusinessException(
                    "Tu correo electrónico no ha sido verificado. " +
                            "Por favor, revisa tu bandeja de entrada y haz clic en el enlace de verificación. " +
                            "Si no recibiste el correo, solicita un nuevo enlace de verificación."
            );
        }

        // 5. Intentar autenticar las credenciales
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            // 6. Autenticación exitosa - Recuperar la entidad completa (por si acaso)
            if (owner == null) {
                throw new BusinessException("No se encontró un usuario con el correo: " + email);
            }

            // 7. Desbloquear la cuenta si estaba bloqueada (login exitoso levanta el bloqueo)
            if (owner.getBlockedUntil() != null) {
                owner.setBlockedUntil(null);
                ownerRepository.save(owner);
                log.info("✅ Cuenta desbloqueada automáticamente tras login exitoso: {}", email);
            }

            // 8. Preparación de datos auxiliares para JWT y Response
            List<String> rolesList = extractRolesAsStringList(owner);

            // 9. Generación del token JWT con las autoridades de Spring Security
            String token = jwtService.generateToken(
                    new User(
                            owner.getEmail(),
                            owner.getPassword(),
                            convertToSimpleGrantedAuthorities(owner)
                    )
            );

            // 10. Registro de auditoría: Login exitoso (en transacción independiente)
            loginAttemptService.registerLoginAttempt(owner, rolesList, true, false, null);

            // 11. Construcción y retorno de la respuesta enriquecida (false = no se envió email en login)
            return buildAuthResponse(owner, token, rolesList, false);

        } catch (BadCredentialsException e) {
            // 12. Autenticación fallida - Delegar al LoginAttemptService
            // Este servicio se ejecuta en transacción independiente (REQUIRES_NEW)
            String errorMessage = loginAttemptService.handleFailedLoginAttempt(email, owner);
            throw new BusinessException(errorMessage);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS - VERIFICACIÓN DE EMAIL
    // =========================================================================

    /**
     * Genera un token de verificación de correo electrónico para el usuario.
     *
     * @param owner la entidad del usuario para quien se genera el token
     * @return el token UUID generado
     */
    private String generateEmailVerificationToken(Owner owner) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(emailVerificationExpirationMinutes);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .email(owner.getEmail())
                .expiresAt(expiresAt)
                .used(false)
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        log.debug("🔑 Token de verificación generado para: {} (expira en {} min)",
                owner.getEmail(), emailVerificationExpirationMinutes);

        return token;
    }

    /**
     * Construye la URL completa de verificación con el token como parámetro.
     *
     * @param token el token UUID de verificación
     * @return la URL completa de verificación
     */
    private String buildVerificationUrl(String token) {
        return backendVerificationUrl + "?token=" + token;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS - BLOQUEO Y UTILIDADES
    // =========================================================================

    /**
     * Verifica si la cuenta de un usuario está actualmente bloqueada.
     *
     * @param owner la entidad del usuario a verificar
     * @return {@code true} si la cuenta está bloqueada y el bloqueo no ha expirado
     */
    private boolean isAccountBlocked(Owner owner) {
        if (owner.getBlockedUntil() == null) {
            return false;
        }
        return owner.getBlockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Extrae los roles de una entidad {@link Owner} y los convierte en una lista de Strings.
     */
    private List<String> extractRolesAsStringList(Owner owner) {
        return owner.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toList());
    }

    /**
     * Convierte los roles de la entidad {@link Owner} en autoridades de Spring Security.
     */
    private List<SimpleGrantedAuthority> convertToSimpleGrantedAuthorities(Owner owner) {
        return owner.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());
    }

    /**
     * Construye el objeto {@link AuthResponse} con todos los datos necesarios
     * para que el frontend gestione la sesión, los permisos y la inactividad.
     *
     * @param owner el usuario autenticado
     * @param token el token JWT generado
     * @param rolesList la lista de roles del usuario
     * @param verificationEmailSent indica si se acaba de enviar un correo de verificación
     * @return el DTO de respuesta de autenticación completo
     */
    private AuthResponse buildAuthResponse(Owner owner, String token, List<String> rolesList, boolean verificationEmailSent) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(owner.getId())
                .email(owner.getEmail())
                .fullName(owner.getFullName())
                .roles(rolesList)
                // 🆕 Campo crucial para que el frontend sepa si debe mostrar pantallas de verificación
                .emailVerified(owner.getEmailVerified())
                // 🆕 Campo nuevo para indicar explícitamente que se envió el correo
                .verificationEmailSent(verificationEmailSent)
                // Campos de inactividad para el control de sesión en el frontend
                .inactivityTimeoutMs(inactivityConfig.getInactivityTimeoutForRoles(rolesList))
                .warningBeforeMs(inactivityConfig.getWarningBeforeMs())
                .build();
    }
}
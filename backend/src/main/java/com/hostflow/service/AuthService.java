package com.hostflow.service;

import com.hostflow.config.InactivityConfig;
import com.hostflow.dto.request.LoginRequest;
import com.hostflow.dto.request.OwnerRegisterRequest;
import com.hostflow.dto.response.AuthResponse;
import com.hostflow.enums.RoleType;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.Owner;
import com.hostflow.model.Role;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de autenticación y registro de usuarios para HostFlow.
 *
 * <p>Orquesta el flujo completo de identidad del usuario, coordinando la interacción
 * entre el repositorio de datos, el codificador de contraseñas, el servicio JWT
 * y el gestor de autenticación de Spring Security.</p>
 *
 * <p>Responsabilidades principales:</p>
 * <ul>
 *   <li>Registro de nuevos dueños de alojamiento con validaciones estrictas de negocio.</li>
 *   <li>Autenticación de usuarios existentes y generación segura de tokens JWT.</li>
 *   <li>Delegación del manejo de intentos fallidos al {@link LoginAttemptService}.</li>
 *   <li>Enriquecimiento de la respuesta de autenticación con metadatos de seguridad
 *       específicos por rol (ej. tiempos de inactividad para el frontend).</li>
 * </ul>
 *
 * <p><strong>Política de Seguridad - Bloqueo de Cuenta:</strong></p>
 * <ul>
 *   <li>Tras 5 intentos fallidos de login, la cuenta se bloquea automáticamente por 1 hora.</li>
 *   <li>Durante el bloqueo, cualquier intento de login es rechazado inmediatamente.</li>
 *   <li>El bloqueo se registra en la tabla {@code login_attempts} para auditoría.</li>
 *   <li>El usuario puede recuperar su contraseña usando el flujo de recuperación existente.</li>
 *   <li>Se informan los intentos restantes antes del bloqueo en la respuesta HTTP.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 3.3.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS (Inmutables gracias a @RequiredArgsConstructor)
    // =========================================================================

    /**
     * Repositorio para operaciones de persistencia de la entidad {@link Owner}.
     */
    private final OwnerRepository ownerRepository;

    /**
     * Repositorio para operaciones de persistencia de la entidad {@link Role}.
     */
    private final RoleRepository roleRepository;

    /**
     * Codificador de contraseñas BCrypt inyectado por Spring Security.
     * Garantiza que las contraseñas nunca se almacenen ni transmitan en texto plano.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Gestor de autenticación de Spring Security.
     * Delega la validación de credenciales al {@link org.springframework.security.core.userdetails.UserDetailsService}.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Servicio encargado de la generación, validación y extracción de claims de tokens JWT.
     */
    private final JwtService jwtService;

    /**
     * Configuración de tiempos de inactividad por rol.
     * Permite definir dinámicamente cuánto tiempo puede estar inactivo un usuario
     * antes de que el frontend cierre su sesión automáticamente, sin modificar la
     * expiración real del token JWT en el backend.
     */
    private final InactivityConfig inactivityConfig;

    /**
     * 🆕 Servicio encargado de gestionar los intentos de login y el bloqueo de cuentas.
     * Se ejecuta en transacciones independientes ({@code REQUIRES_NEW}) para garantizar
     * que los registros de auditoría se persistan incluso si el login lanza una excepción.
     */
    private final LoginAttemptService loginAttemptService;

    // =========================================================================
    // REGISTRO DE USUARIO
    // =========================================================================

    /**
     * Registra un nuevo dueño de alojamiento en el sistema.
     *
     * <p>Flujo del proceso:</p>
     * <ol>
     *   <li>Valida que se hayan aceptado los términos y condiciones.</li>
     *   <li>Verifica que el correo electrónico no esté previamente registrado.</li>
     *   <li>Obtiene o crea el rol {@code ROLE_OWNER} de forma idempotente.</li>
     *   <li>Encripta la contraseña con BCrypt y construye la entidad.</li>
     *   <li>Persiste el nuevo dueño en la base de datos.</li>
     *   <li>Genera un token JWT con los roles y datos del usuario.</li>
     *   <li>Registra el intento de login exitoso en la tabla de auditoría.</li>
     *   <li>Construye la respuesta enriquecida incluyendo los tiempos de inactividad.</li>
     * </ol>
     *
     * @param request los datos de registro del dueño (validados previamente por Bean Validation)
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
        Owner owner = Owner.builder()
                .fullName(request.getFullName())
                .accommodationName(request.getAccommodationName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .termsAccepted(request.getTermsAccepted())
                .enabled(true)
                .blockedUntil(null) // Cuenta nueva, sin bloqueo
                .roles(Set.of(ownerRole))
                .build();

        Owner savedOwner = ownerRepository.save(owner);

        // 4. Preparación de datos auxiliares para JWT y Response
        List<String> rolesList = extractRolesAsStringList(savedOwner);

        // 5. Generación del token JWT con las autoridades de Spring Security
        String token = jwtService.generateToken(
                new User(
                        savedOwner.getEmail(),
                        savedOwner.getPassword(),
                        convertToSimpleGrantedAuthorities(savedOwner)
                )
        );

        // 6. Registro de auditoría: Login exitoso tras registro (en transacción independiente)
        loginAttemptService.registerLoginAttempt(savedOwner, rolesList, true, false, null);

        // 7. Construcción y retorno de la respuesta enriquecida
        return buildAuthResponse(savedOwner, token, rolesList);
    }

    // =========================================================================
    // INICIO DE SESIÓN (LOGIN) CON BLOQUEO DE CUENTA
    // =========================================================================

    /**
     * Autentica un usuario existente y genera un token JWT.
     *
     * <p>Flujo del proceso con seguridad de bloqueo:</p>
     * <ol>
     *   <li>Busca al usuario por email en la base de datos.</li>
     *   <li>Verifica si la cuenta está bloqueada (blockedUntil > ahora).</li>
     *   <li>Si está bloqueada, lanza excepción con tiempo restante de bloqueo.</li>
     *   <li>Intenta autenticar las credenciales con el AuthenticationManager.</li>
     *   <li>Si la autenticación falla:
     *     <ul>
     *       <li>Delega al {@link LoginAttemptService} el manejo del intento fallido.</li>
     *       <li>El servicio registra el intento y verifica si se debe bloquear la cuenta.</li>
     *       <li>Retorna un mensaje de error con los intentos restantes o confirmación de bloqueo.</li>
     *     </ul>
     *   </li>
     *   <li>Si la autenticación es exitosa:
     *     <ul>
     *       <li>Registra el intento exitoso en login_attempts.</li>
     *       <li>Desbloquea la cuenta si estaba bloqueada (blockedUntil = null).</li>
     *       <li>Genera el token JWT.</li>
     *     </ul>
     *   </li>
     *   <li>Retorna la respuesta de autenticación incluyendo los tiempos de inactividad.</li>
     * </ol>
     *
     * <p><strong>Nota de Seguridad:</strong> Para prevenir ataques de enumeración de usuarios,
     * el mensaje de error es genérico cuando el email no existe: "Credenciales incorrectas".
     * Sin embargo, internamente se registra el intento para auditoría.</p>
     *
     * @param request las credenciales del usuario (email y contraseña)
     * @return la respuesta de autenticación con el token JWT y metadatos de sesión
     * @throws BusinessException si las credenciales son incorrectas, el usuario no existe o la cuenta está bloqueada
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();

        // 1. Buscar al usuario en la base de datos
        Owner owner = ownerRepository.findByEmail(email).orElse(null);

        // 2. Verificar si la cuenta está bloqueada
        if (owner != null && isAccountBlocked(owner)) {
            LocalDateTime blockedUntil = owner.getBlockedUntil();
            long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), blockedUntil).toMinutes();

            // Registrar intento de login con cuenta bloqueada (en transacción independiente)
            List<String> rolesList = owner != null ? extractRolesAsStringList(owner) : List.of();
            loginAttemptService.registerLoginAttempt(owner, rolesList, false, false, blockedUntil);

            throw new BusinessException(
                    String.format("Cuenta bloqueada por seguridad. Intenta nuevamente en %d minutos. " +
                            "Si olvidaste tu contraseña, usa la opción 'Recuperar contraseña'.", minutesRemaining)
            );
        }

        // 3. Intentar autenticar las credenciales
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            // 4. Autenticación exitosa - Recuperar la entidad completa
            if (owner == null) {
                throw new BusinessException("No se encontró un usuario con el correo: " + email);
            }

            // 5. Desbloquear la cuenta si estaba bloqueada (login exitoso)
            if (owner.getBlockedUntil() != null) {
                owner.setBlockedUntil(null);
                ownerRepository.save(owner);
                log.info("✅ Cuenta desbloqueada automáticamente tras login exitoso: {}", email);
            }

            // 6. Preparación de datos auxiliares para JWT y Response
            List<String> rolesList = extractRolesAsStringList(owner);

            // 7. Generación del token JWT con las autoridades de Spring Security
            String token = jwtService.generateToken(
                    new User(
                            owner.getEmail(),
                            owner.getPassword(),
                            convertToSimpleGrantedAuthorities(owner)
                    )
            );

            // 8. Registro de auditoría: Login exitoso (en transacción independiente)
            loginAttemptService.registerLoginAttempt(owner, rolesList, true, false, null);

            // 9. Construcción y retorno de la respuesta enriquecida
            return buildAuthResponse(owner, token, rolesList);

        } catch (BadCredentialsException e) {
            // 10. Autenticación fallida - Delegar al LoginAttemptService
            // Este servicio se ejecuta en transacción independiente (REQUIRES_NEW)
            String errorMessage = loginAttemptService.handleFailedLoginAttempt(email, owner);
            throw new BusinessException(errorMessage);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Verifica si la cuenta de un usuario está actualmente bloqueada.
     *
     * @param owner la entidad del usuario a verificar
     * @return {@code true} si la cuenta está bloqueada y el bloqueo no ha expirado,
     *         {@code false} si no está bloqueada o el bloqueo ya expiró
     */
    private boolean isAccountBlocked(Owner owner) {
        if (owner.getBlockedUntil() == null) {
            return false;
        }
        return owner.getBlockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Extrae los roles de una entidad {@link Owner} y los convierte en una lista de Strings.
     *
     * @param owner la entidad del dueño
     * @return lista de nombres de roles en formato String (ej. ["ROLE_OWNER"])
     */
    private List<String> extractRolesAsStringList(Owner owner) {
        return owner.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toList());
    }

    /**
     * Convierte los roles de la entidad {@link Owner} en autoridades de Spring Security.
     *
     * @param owner la entidad del dueño
     * @return lista de {@link SimpleGrantedAuthority} requerida por Spring Security
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
     * @param owner     la entidad del dueño autenticado
     * @param token     el token JWT generado
     * @param rolesList la lista de roles en formato String
     * @return el DTO de respuesta de autenticación completo
     */
    private AuthResponse buildAuthResponse(Owner owner, String token, List<String> rolesList) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(owner.getId())
                .email(owner.getEmail())
                .fullName(owner.getFullName())
                .roles(rolesList)
                // Campos de inactividad para el control de sesión en el frontend
                .inactivityTimeoutMs(inactivityConfig.getInactivityTimeoutForRoles(rolesList))
                .warningBeforeMs(inactivityConfig.getWarningBeforeMs())
                .build();
    }
}
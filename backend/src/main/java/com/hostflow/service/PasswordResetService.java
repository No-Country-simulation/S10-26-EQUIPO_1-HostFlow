package com.hostflow.service;

import com.hostflow.dto.request.ForgotPasswordRequest;
import com.hostflow.dto.request.ResetPasswordRequest;
import com.hostflow.dto.response.PasswordResetResponse;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Owner;
import com.hostflow.model.PasswordResetToken;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Servicio para la gestión de recuperación de contraseñas en HostFlow.
 *
 * <p>Maneja el flujo completo de recuperación de contraseña:</p>
 * <ol>
 *   <li>Generación de código de recuperación de 6 dígitos</li>
 *   <li>Envío del código por correo electrónico</li>
 *   <li>Validación del código ingresado por el usuario</li>
 *   <li>Actualización de la contraseña en la base de datos</li>
 *   <li>Invalidación del token después del uso</li>
 * </ol>
 *
 * <p>Características de seguridad:</p>
 * <ul>
 *   <li>Tokens de un solo uso (se marcan como utilizados)</li>
 *   <li>Expiración configurable (por defecto 60 minutos)</li>
 *   <li>Invalidación de tokens anteriores al generar uno nuevo</li>
 *   <li>No revela si el email existe o no (previene enumeración)</li>
 * </ul>
 *
 * @author HostFlow Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    /**
     * Repositorio de dueños de alojamiento.
     */
    private final OwnerRepository ownerRepository;

    /**
     * Repositorio de tokens de recuperación de contraseña.
     */
    private final PasswordResetTokenRepository tokenRepository;

    /**
     * Servicio de envío de correos electrónicos.
     */
    private final EmailService emailService;

    /**
     * Codificador de contraseñas BCrypt.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Tiempo de expiración del token en minutos.
     * Se configura en application.yml (por defecto: 60 minutos).
     */
    @Value("${app.password-reset.expiration-minutes:60}")
    private int expirationMinutes;

    /**
     * Longitud del código de recuperación.
     * Por defecto: 6 dígitos.
     */
    @Value("${app.password-reset.code-length:6}")
    private int codeLength;

    /**
     * Solicita el envío de un código de recuperación de contraseña.
     *
     * <p>Flujo del proceso:</p>
     * <ol>
     *   <li>Verifica que el correo electrónico esté registrado.</li>
     *   <li>Invalida tokens anteriores pendientes para ese correo.</li>
     *   <li>Genera un nuevo código numérico de 6 dígitos.</li>
     *   <li>Calcula la fecha de expiración.</li>
     *   <li>Persiste el token en la base de datos.</li>
     *   <li>Envía el código por correo electrónico.</li>
     *   <li>Retorna confirmación (sin revelar si el email existe).</li>
     * </ol>
     *
     * <p><strong>Nota de seguridad:</strong> Siempre retorna éxito incluso
     * si el email no está registrado, para prevenir ataques de enumeración
     * de usuarios.</p>
     *
     * @param request la solicitud con el correo electrónico
     * @return respuesta de confirmación con tiempo de expiración
     */
    @Transactional
    public PasswordResetRequestResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();

        // Verificar si el correo está registrado
        Owner owner = ownerRepository.findByEmail(email).orElse(null);

        if (owner == null) {
            // Por seguridad, no revelar que el email no existe
            // Siempre retornamos éxito para prevenir enumeración de usuarios
            log.warn("⚠️ Solicitud de recuperación para email no registrado: {}", email);
            return PasswordResetRequestResponse.builder()
                    .message("Si el correo está registrado, recibirás un código de recuperación.")
                    .email(obfuscateEmail(email))
                    .expirationMinutes(expirationMinutes)
                    .build();
        }

        // Invalidar tokens anteriores pendientes para este correo
        var oldTokens = tokenRepository.findByEmailAndUsedFalse(email);
        if (!oldTokens.isEmpty()) {
            tokenRepository.deleteAll(oldTokens);
            log.info("🗑️ Tokens anteriores invalidados para: {}", email);
        }

        // Generar código de recuperación de 6 dígitos
        String resetCode = generateResetCode();

        // Calcular fecha de expiración
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        // Crear y persistir el token
        PasswordResetToken token = PasswordResetToken.builder()
                .token(resetCode)
                .email(email)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        tokenRepository.save(token);

        // Enviar email con el código
        emailService.sendPasswordResetEmail(email, resetCode, expirationMinutes);

        log.info("✅ Código de recuperación generado para: {} (expira en {} min)", email, expirationMinutes);

        return PasswordResetRequestResponse.builder()
                .message("Si el correo está registrado, recibirás un código de recuperación.")
                .email(obfuscateEmail(email))
                .expirationMinutes(expirationMinutes)
                .build();
    }

    /**
     * Restablece la contraseña utilizando el código de recuperación.
     *
     * <p>Flujo del proceso:</p>
     * <ol>
     *   <li>Busca el token por código y email.</li>
     *   <li>Valida que el token exista.</li>
     *   <li>Valida que el token no haya expirado.</li>
     *   <li>Valida que el token no haya sido utilizado.</li>
     *   <li>Busca al usuario por email.</li>
     *   <li>Actualiza la contraseña con BCrypt.</li>
     *   <li>Marca el token como utilizado.</li>
     *   <li>Envía email de confirmación.</li>
     * </ol>
     *
     * @param request la solicitud con email, código y nueva contraseña
     * @return respuesta de confirmación del cambio
     * @throws BusinessException si el token es inválido, expirado o ya usado
     * @throws ResourceNotFoundException si no se encuentra el usuario
     */
    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String code = request.getCode();
        String newPassword = request.getNewPassword();

        // Buscar el token por código y email
        PasswordResetToken token = tokenRepository.findByTokenAndEmail(code, email)
                .orElseThrow(() -> new BusinessException(
                        "El código de recuperación es inválido o no corresponde a este correo."));

        // Validar que el token no haya expirado
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "El código de recuperación ha expirado. Solicita un nuevo código.");
        }

        // Validar que el token no haya sido utilizado
        if (Boolean.TRUE.equals(token.getUsed())) {
            throw new BusinessException(
                    "El código de recuperación ya fue utilizado. Solicita un nuevo código.");
        }

        // Buscar al usuario
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró un usuario con el correo: " + email));

        // Actualizar la contraseña con BCrypt
        owner.setPassword(passwordEncoder.encode(newPassword));
        ownerRepository.save(owner);

        // Marcar el token como utilizado
        token.setUsed(true);
        tokenRepository.save(token);

        // Enviar email de confirmación
        emailService.sendPasswordChangedConfirmation(email);

        log.info("✅ Contraseña restablecida exitosamente para: {}", email);

        return PasswordResetResponse.builder()
                .message("Tu contraseña ha sido actualizada exitosamente. Ya puedes iniciar sesión.")
                .email(obfuscateEmail(email))
                .expirationMinutes(expirationMinutes)
                .build();
    }

    /**
     * Genera un código numérico aleatorio de la longitud configurada.
     *
     * <p>El código se genera con dígitos del 0 al 9 y se rellena
     * con ceros a la izquierda si es necesario para mantener
     * la longitud exacta.</p>
     *
     * @return el código de recuperación generado
     */
    private String generateResetCode() {
        Random random = new Random();
        int maxCode = (int) Math.pow(10, codeLength);
        int code = random.nextInt(maxCode);
        // Rellenar con ceros a la izquierda para mantener la longitud
        return String.format("%0" + codeLength + "d", code);
    }

    /**
     * Ofusca el correo electrónico para mostrarlo parcialmente
     * en las respuestas (medida de seguridad).
     *
     * <p>Ejemplo: "usuario@dominio.com" → "u***@d***.com"</p>
     *
     * @param email el correo electrónico a ofuscar
     * @return el correo ofuscado
     */
    private String obfuscateEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        String obfuscatedLocal = localPart.charAt(0) + "***";
        String[] domainParts = domainPart.split("\\.");
        String obfuscatedDomain = domainParts[0].charAt(0) + "***." + domainParts[1];

        return obfuscatedLocal + "@" + obfuscatedDomain;
    }

    /**
     * DTO interno para la respuesta de solicitud de recuperación.
     * Se define aquí para mantener la cohesión del servicio.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PasswordResetRequestResponse {
        private String message;
        private String email;
        private Integer expirationMinutes;
    }
}
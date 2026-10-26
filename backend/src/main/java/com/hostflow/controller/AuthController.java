package com.hostflow.controller;

import com.hostflow.dto.request.ForgotPasswordRequest;
import com.hostflow.dto.request.LoginRequest;
import com.hostflow.dto.request.OwnerRegisterRequest;
import com.hostflow.dto.request.ResetPasswordRequest;
import com.hostflow.dto.response.AuthResponse;
import com.hostflow.dto.response.PasswordResetResponse;
import com.hostflow.service.AuthService;
import com.hostflow.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para las operaciones de autenticación en HostFlow.
 *
 * <p>Expone los endpoints públicos para el registro de nuevos dueños
 * de alojamiento, el inicio de sesión de usuarios existentes y la
 * recuperación de contraseñas olvidadas. Estos endpoints no requieren
 * autenticación previa.</p>
 *
 * <p>Endpoints disponibles:</p>
 * <ul>
 *   <li>{@code POST /auth/register} - Registro de nuevo dueño</li>
 *   <li>{@code POST /auth/login} - Inicio de sesión</li>
 *   <li>{@code POST /auth/forgot-password} - Solicitar código de recuperación</li>
 *   <li>{@code POST /auth/reset-password} - Restablecer contraseña con código</li>
 * </ul>
 *
 * <p>Este controlador maneja todas las operaciones relacionadas con
 * la identidad del usuario, desde el registro inicial hasta la
 * recuperación de acceso cuando el usuario olvida sus credenciales.</p>
 *
 * @author HostFlow Team
 * @version 1.1.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para registro, autenticación y recuperación de usuarios")
public class AuthController {

    /**
     * Servicio de autenticación y registro de dueños de alojamiento.
     * Maneja el registro de nuevos usuarios y el proceso de login
     * con generación de tokens JWT.
     */
    private final AuthService authService;

    /**
     * Servicio de recuperación de contraseñas.
     * Maneja la generación de códigos de recuperación, el envío
     * de emails y el restablecimiento de contraseñas.
     */
    private final PasswordResetService passwordResetService;

    // =========================================================================
    // REGISTRO DE NUEVO USUARIO
    // =========================================================================

    /**
     * Registra un nuevo dueño de alojamiento en el sistema.
     *
     * <p>Este endpoint es público y no requiere autenticación.
     * Al completar el registro exitosamente, se retorna un token
     * JWT que el cliente debe utilizar en las peticiones posteriores.</p>
     *
     * <p>Flujo del registro:</p>
     * <ol>
     *   <li>Valida los datos de entrada (Bean Validation)</li>
     *   <li>Verifica que el email no esté registrado</li>
     *   <li>Valida la aceptación de términos y condiciones</li>
     *   <li>Encripta la contraseña con BCrypt</li>
     *   <li>Asigna el rol ROLE_OWNER al nuevo usuario</li>
     *   <li>Persiste el usuario en la base de datos</li>
     *   <li>Genera y retorna el token JWT</li>
     * </ol>
     *
     * @param request los datos de registro del dueño (validados)
     * @return respuesta HTTP 201 con el token JWT y datos del usuario
     */
    @PostMapping("/register")
    @Operation(
            summary = "Registrar nuevo dueño de alojamiento",
            description = """
            Registra un nuevo dueño de alojamiento en HostFlow.
            
            Los datos requeridos son:
            - Nombre y apellido del dueño
            - Nombre del alojamiento
            - Correo electrónico (debe ser único)
            - Contraseña (mínimo 8 caracteres)
            - Aceptación de términos y política de privacidad
            
            Retorna un token JWT para autenticación en peticiones posteriores.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registro exitoso"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o correo ya registrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<AuthResponse> registerOwner(
            @Valid @RequestBody OwnerRegisterRequest request) {

        AuthResponse response = authService.registerOwner(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // INICIO DE SESIÓN
    // =========================================================================

    /**
     * Autentica un usuario existente y genera un token JWT.
     *
     * <p>Este endpoint es público y no requiere autenticación.
     * Valida las credenciales del usuario y, si son correctas,
     * retorna un token JWT para acceder a los recursos protegidos.</p>
     *
     * <p>Flujo de autenticación:</p>
     * <ol>
     *   <li>Recibe email y contraseña del cliente</li>
     *   <li>Valida las credenciales contra la base de datos</li>
     *   <li>Si son correctas, genera un token JWT</li>
     *   <li>Retorna el token junto con la información del usuario</li>
     * </ol>
     *
     * <p><strong>Importante:</strong> El token JWT debe enviarse en el header
     * {@code Authorization: Bearer <token>} en todas las peticiones
     * posteriores a endpoints protegidos.</p>
     *
     * @param request las credenciales del usuario (email y contraseña)
     * @return respuesta HTTP 200 con el token JWT y datos del usuario
     */
    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = """
            Autentica a un usuario existente en HostFlow.
            
            Requiere:
            - Correo electrónico registrado
            - Contraseña válida
            
            Retorna un token JWT para autenticación en peticiones posteriores.
            El token debe enviarse en el header: Authorization: Bearer <token>
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // RECUPERACIÓN DE CONTRASEÑA - SOLICITUD DE CÓDIGO
    // =========================================================================

    /**
     * Solicita el envío de un código de recuperación de contraseña.
     *
     * <p>Este endpoint es público y no requiere autenticación.
     * Cuando un usuario olvida su contraseña, puede solicitar un
     * código de recuperación de 6 dígitos que se enviará a su
     * correo electrónico registrado.</p>
     *
     * <p>Flujo de recuperación:</p>
     * <ol>
     *   <li>El usuario ingresa su correo electrónico</li>
     *   <li>El sistema verifica si el correo está registrado</li>
     *   <li>Se genera un código numérico de 6 dígitos</li>
     *   <li>Se calcula la fecha de expiración (60 minutos)</li>
     *   <li>Se persiste el token en la base de datos</li>
     *   <li>Se envía el código por correo electrónico</li>
     *   <li>Se retorna confirmación al cliente</li>
     * </ol>
     *
     * <p><strong>Nota de seguridad:</strong> Por prevención de ataques
     * de enumeración de usuarios, este endpoint siempre retorna éxito
     * incluso si el correo electrónico no está registrado. El mensaje
     * es genérico: "Si el correo está registrado, recibirás un código".</p>
     *
     * <p>Características del código:</p>
     * <ul>
     *   <li>Longitud: 6 dígitos numéricos</li>
     *   <li>Expiración: 60 minutos</li>
     *   <li>Un solo uso (se invalida después de usarlo)</li>
     *   <li>Al solicitar un nuevo código, se invalidan los anteriores</li>
     * </ul>
     *
     * @param request la solicitud con el correo electrónico del usuario
     * @return respuesta HTTP 200 con confirmación y tiempo de expiración
     */
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = """
            Solicita el envío de un código de recuperación de contraseña.
            
            El código de 6 dígitos se enviará al correo electrónico proporcionado.
            El código expira en 60 minutos y es de un solo uso.
            
            Por seguridad, siempre retorna éxito incluso si el email no está registrado.
            Esto previene ataques de enumeración de usuarios.
            
            Flujo:
            1. Usuario ingresa su correo electrónico
            2. Sistema genera código de 6 dígitos
            3. Código se envía por email
            4. Usuario ingresa código en el frontend
            5. Usuario establece nueva contraseña
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código enviado exitosamente (o email no registrado)"),
            @ApiResponse(responseCode = "400", description = "Email inválido o formato incorrecto"),
            @ApiResponse(responseCode = "500", description = "Error al enviar el email")
    })
    public ResponseEntity<PasswordResetService.PasswordResetRequestResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        PasswordResetService.PasswordResetRequestResponse response =
                passwordResetService.forgotPassword(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // RECUPERACIÓN DE CONTRASEÑA - RESTABLECIMIENTO
    // =========================================================================

    /**
     * Restablece la contraseña utilizando el código de recuperación.
     *
     * <p>Este endpoint es público y no requiere autenticación.
     * Valida el código de recuperación ingresado por el usuario
     * y, si es válido, actualiza la contraseña en la base de datos.</p>
     *
     * <p>Flujo de restablecimiento:</p>
     * <ol>
     *   <li>Usuario ingresa email, código de 6 dígitos y nueva contraseña</li>
     *   <li>Sistema busca el token por código y email</li>
     *   <li>Valida que el token exista</li>
     *   <li>Valida que el token no haya expirado (60 min)</li>
     *   <li>Valida que el token no haya sido utilizado previamente</li>
     *   <li>Busca al usuario por email</li>
     *   <li>Encripta la nueva contraseña con BCrypt</li>
     *   <li>Actualiza la contraseña en la base de datos</li>
     *   <li>Marca el token como utilizado (un solo uso)</li>
     *   <li>Envía email de confirmación al usuario</li>
     *   <li>Retorna confirmación de éxito</li>
     * </ol>
     *
     * <p>Validaciones de seguridad:</p>
     * <ul>
     *   <li>El código debe coincidir exactamente con el generado</li>
     *   <li>El código debe corresponder al email proporcionado</li>
     *   <li>El código no debe haber expirado (máximo 60 minutos)</li>
     *   <li>El código no debe haber sido utilizado anteriormente</li>
     *   <li>La nueva contraseña debe tener mínimo 8 caracteres</li>
     * </ul>
     *
     * <p>Después del restablecimiento exitoso:</p>
     * <ul>
     *   <li>El usuario puede iniciar sesión con la nueva contraseña</li>
     *   <li>El código utilizado queda invalidado permanentemente</li>
     *   <li>Se envía email de confirmación al usuario</li>
     * </ul>
     *
     * @param request la solicitud con email, código y nueva contraseña
     * @return respuesta HTTP 200 con confirmación del cambio
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Restablecer contraseña con código de recuperación",
            description = """
            Restablece la contraseña utilizando el código de recuperación recibido por email.
            
            Requiere:
            - Correo electrónico registrado
            - Código de recuperación de 6 dígitos (recibido por email)
            - Nueva contraseña (mínimo 8 caracteres)
            
            El código es de un solo uso y expira en 60 minutos.
            Después del restablecimiento exitoso, el usuario puede iniciar sesión
            con la nueva contraseña.
            
            Flujo:
            1. Usuario recibe código por email (6 dígitos)
            2. Usuario ingresa código + nueva contraseña
            3. Sistema valida código y actualiza contraseña
            4. Usuario recibe confirmación por email
            5. Usuario puede iniciar sesión con nueva contraseña
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado o ya utilizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        PasswordResetResponse response = passwordResetService.resetPassword(request);

        return ResponseEntity.ok(response);
    }
}
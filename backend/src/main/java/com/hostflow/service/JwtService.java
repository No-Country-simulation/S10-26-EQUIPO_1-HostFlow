package com.hostflow.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio centralizado para la gestión de tokens JWT (JSON Web Token).
 *
 * <p>Responsabilidades principales:</p>
 * <ul>
 *   <li>Generación de tokens de acceso y refresco firmados criptográficamente.</li>
 *   <li>Extracción segura de claims (datos) desde un token válido.</li>
 *   <li>Validación de integridad y vigencia de los tokens.</li>
 * </ul>
 *
 * <p><strong>Nota de Arquitectura:</strong> Este servicio gestiona la expiración
 * criptográfica del token (ej. 24 horas). El control de "inactividad del usuario"
 * (ej. 10 minutos) se maneja de forma independiente en el frontend mediante los
 * valores enviados en {@link com.hostflow.dto.response.AuthResponse}, sin alterar
 * la validez nativa del JWT en el backend.</p>
 *
 * <p>Utiliza el algoritmo HS512 (HMAC-SHA512) para la firma, que requiere una
 * clave secreta de al menos 64 bytes (512 bits) para cumplir con el estándar JWA.</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Service
public class JwtService {

    // =========================================================================
    // CONFIGURACIÓN INYECTADA DESDE application.properties
    // =========================================================================

    /**
     * Clave secreta utilizada para firmar y verificar los tokens JWT.
     * Debe ser una cadena de al menos 64 caracteres para HS512.
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Tiempo de vida (TTL) del token de acceso en milisegundos.
     * Configurado por defecto en 24 horas (86,400,000 ms).
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Tiempo de vida (TTL) del token de refresco en milisegundos.
     * Configurado por defecto en 7 días (604,800,000 ms).
     */
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Identificador del emisor (Claim "iss") en el payload del JWT.
     * Útil para auditar qué sistema generó el token.
     */
    @Value("${jwt.issuer}")
    private String issuer;

    // =========================================================================
    // MÉTODOS DE EXTRACCIÓN DE DATOS (CLAIMS)
    // =========================================================================

    /**
     * Extrae el nombre de usuario (generalmente el email) del token JWT.
     *
     * @param token el token JWT a analizar
     * @return el valor del claim "subject" (email del usuario)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha y hora de expiración del token JWT.
     *
     * @param token el token JWT a analizar
     * @return la fecha de expiración contenida en el claim "exp"
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token JWT utilizando una función de resolución.
     * Este método genérico permite extraer cualquier dato personalizado (roles, ID, etc.).
     *
     * @param token          el token JWT a analizar
     * @param claimsResolver función que define qué claim extraer (ej. Claims::getSubject)
     * @param <T>            el tipo de dato esperado del claim
     * @return el valor del claim solicitado
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea y extrae todos los claims del token JWT.
     * Utiliza la clave secreta para verificar la firma criptográfica antes de devolver el payload.
     *
     * @param token el token JWT a analizar
     * @return el objeto {@link Claims} con todos los datos del payload
     * @throws io.jsonwebtoken.JwtException si el token es inválido, está mal formado o la firma no coincide
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // =========================================================================
    // MÉTODOS DE GENERACIÓN DE TOKENS
    // =========================================================================

    /**
     * Genera un token JWT de acceso estándar para el usuario especificado.
     *
     * @param userDetails los detalles del usuario autenticado por Spring Security
     * @return el token JWT generado como cadena de texto (String)
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token JWT de acceso con claims personalizados adicionales.
     *
     * @param extraClaims mapa de claims adicionales a incluir en el payload (ej. roles, permisos)
     * @param userDetails los detalles del usuario autenticado por Spring Security
     * @return el token JWT generado como cadena de texto (String)
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Genera un token JWT de refresco (Refresh Token) para renovar la sesión
     * sin requerir que el usuario ingrese sus credenciales nuevamente.
     *
     * @param userDetails los detalles del usuario autenticado por Spring Security
     * @return el token JWT de refresco generado como cadena de texto (String)
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    /**
     * Construye el token JWT aplicando todos los parámetros y la firma criptográfica.
     *
     * @param extraClaims claims personalizados a incluir en el payload
     * @param userDetails detalles del usuario (provee el subject/username)
     * @param expiration  tiempo de vida del token en milisegundos
     * @return el token JWT compacto y firmado
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)                                     // Claims personalizados
                .subject(userDetails.getUsername())                      // Claim "sub": Identificador del usuario
                .issuer(issuer)                                          // Claim "iss": Emisor del token
                .issuedAt(new Date(System.currentTimeMillis()))          // Claim "iat": Fecha de emisión
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Claim "exp": Fecha de expiración
                .signWith(getSigningKey())                               // Firma criptográfica con HS512
                .compact();                                              // Serializa a formato String compacto (Header.Payload.Signature)
    }

    // =========================================================================
    // MÉTODOS DE VALIDACIÓN
    // =========================================================================

    /**
     * Valida un token JWT verificando dos condiciones críticas:
     * <ol>
     *   <li>El nombre de usuario en el token coincide con el del {@link UserDetails} proporcionado.</li>
     *   <li>El token no ha superado su fecha de expiración (claim "exp").</li>
     * </ol>
     *
     * @param token       el token JWT a validar
     * @param userDetails los detalles del usuario contra los cuales se valida
     * @return {@code true} si el token es válido y pertenece al usuario; {@code false} en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Verifica internamente si la fecha de expiración del token es anterior a la fecha actual.
     *
     * @param token el token JWT a verificar
     * @return {@code true} si el token ya expiró, {@code false} si aún es válido en el tiempo
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE SEGURIDAD
    // =========================================================================

    /**
     * Obtiene la clave secreta como un objeto {@link SecretKey} compatible con JJWT.
     *
     * <p>Convierte la cadena de texto configurada en un array de bytes UTF-8
     * y la procesa mediante {@link Keys#hmacShaKeyFor(byte[])} para garantizar
     * que cumpla con los requisitos de longitud del algoritmo HS512 (mínimo 64 bytes).</p>
     *
     * @return la clave secreta lista para firmar o verificar tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
package com.hostflow.service;

import io.jsonwebtoken.*;
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
 * Servicio encargado de la generación, validación y extracción
 * de datos desde tokens JWT (JSON Web Token).
 *
 * <p>Utiliza el algoritmo HS512 (HMAC-SHA512) para firmar los tokens,
 * proporcionando un alto nivel de seguridad. Los tokens contienen
 * la información del usuario autenticado y su fecha de expiración.</p>
 *
 * <p>Flujo de uso:</p>
 * <ol>
 *   <li>El usuario se autentica con email y contraseña.</li>
 *   <li>Se genera un JWT con la información del usuario.</li>
 *   <li>El cliente envía el token en cada petición subsequente.</li>
 *   <li>Este servicio valida el token y extrae la información.</li>
 * </ol>
 *
 * @author HostFlow Team
 */
@Service
public class JwtService {

    /**
     * Clave secreta utilizada para firmar y verificar los tokens JWT.
     * Se inyecta desde la configuración de la aplicación.
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Tiempo de expiración del token de acceso en milisegundos.
     * Por defecto: 24 horas (86400000 ms).
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Tiempo de expiración del token de refresh en milisegundos.
     * Por defecto: 7 días (604800000 ms).
     */
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Emisor del token JWT.
     */
    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * Obtiene la clave secreta como un objeto {@link SecretKey}.
     * La clave se decodifica desde Base64 o se usa directamente
     * si tiene la longitud mínima requerida por HS512 (64 bytes).
     *
     * @return la clave secreta para firmar tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extrae el nombre de usuario (email) del token JWT.
     *
     * @param token el token JWT a analizar
     * @return el email del usuario contenido en el token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración del token JWT.
     *
     * @param token el token JWT a analizar
     * @return la fecha de expiración del token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token JWT utilizando
     * una función de resolución.
     *
     * @param token          el token JWT a analizar
     * @param claimsResolver función que extrae el claim deseado
     * @param <T>            tipo del claim a extraer
     * @return el valor del claim solicitado
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token JWT.
     * Utiliza la clave secreta para verificar la firma.
     *
     * @param token el token JWT a analizar
     * @return todos los claims contenidos en el token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica si el token JWT ha expirado.
     *
     * @param token el token JWT a verificar
     * @return {@code true} si el token ha expirado, {@code false} en caso contrario
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Genera un token JWT de acceso para el usuario especificado.
     *
     * @param userDetails los detalles del usuario autenticado
     * @return el token JWT generado como String
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token JWT de acceso con claims adicionales.
     *
     * @param extraClaims claims adicionales a incluir en el token
     * @param userDetails los detalles del usuario autenticado
     * @return el token JWT generado como String
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Genera un token JWT de refresh para renovar la sesión.
     *
     * @param userDetails los detalles del usuario autenticado
     * @return el token JWT de refresh generado como String
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    /**
     * Construye un token JWT con los parámetros especificados.
     *
     * @param extraClaims claims adicionales a incluir
     * @param userDetails detalles del usuario
     * @param expiration  tiempo de expiración en milisegundos
     * @return el token JWT construido
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuer(issuer)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Valida un token JWT verificando su firma y que no haya expirado.
     *
     * @param token       el token JWT a validar
     * @param userDetails los detalles del usuario para comparar
     * @return {@code true} si el token es válido, {@code false} en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }
}
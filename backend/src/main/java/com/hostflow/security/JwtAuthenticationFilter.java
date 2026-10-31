package com.hostflow.security;

import com.hostflow.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación JWT que se ejecuta en cada petición HTTP.
 *
 * <p>Este filtro intercepta todas las peticiones entrantes, extrae el token
 * JWT del header {@code Authorization}, lo valida y, si es válido,
 * establece el contexto de seguridad de Spring con la información
 * del usuario autenticado.</p>
 *
 * <p>Extiende {@link OncePerRequestFilter} para garantizar que el filtro
 * se ejecute exactamente una vez por petición, incluso en caso de
 * forwards internos.</p>
 *
 * @author HostFlow Team
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Servicio para operaciones con tokens JWT.
     */
    private final JwtService jwtService;

    /**
     * Servicio para cargar los detalles del usuario.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Procesa cada petición HTTP filtrada.
     *
     * <p>Flujo del filtro:</p>
     * <ol>
     *   <li>Verifica si la petición es para un endpoint público (sin autenticación).</li>
     *   <li>Extrae el token JWT del header {@code Authorization}.</li>
     *   <li>Extrae el email del token.</li>
     *   <li>Si el email es válido y no hay autenticación previa en el contexto:</li>
     *   <ol>
     *     <li>Carga los detalles del usuario.</li>
     *     <li>Valida el token contra los detalles del usuario.</li>
     *     <li>Si es válido, establece la autenticación en el contexto de seguridad.</li>
     *   </ol>
     *   <li>Continúa la cadena de filtros.</li>
     * </ol>
     *
     * @param request     la petición HTTP entrante
     * @param response    la respuesta HTTP saliente
     * @param filterChain la cadena de filtros a ejecutar
     * @throws ServletException si ocurre un error en el servlet
     * @throws IOException      si ocurre un error de I/O
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Obtener la URI de la petición para verificar si es un endpoint público
        final String requestPath = request.getRequestURI();

        // Verificar si la petición corresponde a un endpoint que no requiere autenticación
        // Estos endpoints permiten acceso público para registro, login y recuperación de contraseña
        if (isPublicEndpoint(requestPath)) {
            // Continuar con la cadena de filtros sin validar token JWT
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token JWT del header Authorization
        final String authHeader = request.getHeader("Authorization");

        // Si no hay header o no comienza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token (remover el prefijo "Bearer ")
        final String jwt = authHeader.substring(7);

        // Extraer el email del token JWT
        final String userEmail = jwtService.extractUsername(jwt);

        // Si el email es válido y no hay autenticación previa en el contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Cargar los detalles del usuario desde la base de datos
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Validar el token contra los detalles del usuario
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Crear el objeto de autenticación con los datos del usuario y sus roles
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // No se requiere contraseña ya que el token es válido
                                userDetails.getAuthorities()
                        );

                // Establecer los detalles de la petición web (IP, sesión, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Establecer la autenticación en el contexto de seguridad de Spring
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continuar con la cadena de filtros para procesar la petición
        filterChain.doFilter(request, response);
    }

    /**
     * Verifica si la petición corresponde a un endpoint público que no requiere autenticación.
     *
     * @param path la URI de la petición
     * @return true si el endpoint es público, false si requiere autenticación
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1/auth/register") ||
                path.startsWith("/api/v1/auth/login") ||
                path.startsWith("/api/v1/auth/forgot-password") ||
                path.startsWith("/api/v1/auth/reset-password") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api-docs");
    }
}
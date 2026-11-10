package com.hostflow.config;

import com.hostflow.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración principal de seguridad para la aplicación HostFlow.
 *
 * <p>Esta clase centraliza toda la configuración de Spring Security, incluyendo:</p>
 * <ul>
 *   <li>La cadena de filtros de seguridad ({@link SecurityFilterChain}).</li>
 *   <li>El proveedor de autenticación y el gestor de autenticación.</li>
 *   <li>Las reglas de autorización jerárquicas para endpoints públicos y protegidos.</li>
 *   <li>La configuración CORS para permitir peticiones desde el frontend.</li>
 * </ul>
 *
 * <p><strong>⚠️ Nota crítica sobre el orden de las reglas:</strong></p>
 * <p>Spring Security evalúa las reglas de {@code requestMatchers} de arriba hacia abajo.
 * La primera regla que coincida con la URL es la que se aplica. Por lo tanto, las rutas
 * más específicas deben declararse <strong>ANTES</strong> que las rutas generales
 * (ej. {@code /owners/accommodation/**} debe ir antes que {@code /owners/**}).</p>
 *
 * @author HostFlow Team
 * @version 2.3.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    /**
     * El PasswordEncoder se inyecta desde la configuración existente (ej. PasswordEncoderConfig).
     * No se define un nuevo bean aquí para evitar conflictos de duplicidad (Duplicate Bean Error).
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Configura la cadena de filtros de seguridad de Spring Security.
     *
     * @param http el objeto {@link HttpSecurity} para configurar la seguridad web
     * @return la cadena de filtros de seguridad configurada
     * @throws Exception si ocurre un error en la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 1. Deshabilitar CSRF (no necesario para APIs REST stateless con JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Configurar CORS (Cross-Origin Resource Sharing)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Configurar autorización de peticiones (ORDEN CRÍTICO)
                .authorizeHttpRequests(auth -> auth

                        // ==========================================
                        // NIVEL 1: ENDPOINTS PÚBLICOS (sin autenticación)
                        // Se incluyen TODAS las rutas necesarias para acceso abierto y Swagger UI
                        // ==========================================
                        .requestMatchers(
                                "/auth/**",                                // Registro, login, recuperación de contraseña

                                "/public/**",                              // ✅ CORRECCIÓN: Endpoints públicos de consulta (ej. alojamientos, servicios)

                                // Swagger UI & OpenAPI (Springdoc)
                                "/v3/api-docs/**",                         // Documentación OpenAPI JSON (wildcard)
                                "/v3/api-docs",                            // Documentación OpenAPI JSON (ruta base)
                                "/swagger-ui/**",                          // Interfaz Swagger UI (archivos estáticos)
                                "/swagger-ui.html",                        // Página principal de Swagger (redirección)
                                "/swagger-ui/index.html",                  // Índice de Swagger UI
                                "/api-docs/**",                            // Rutas alternativas de API docs
                                "/api-docs/swagger-config",                // Configuración específica de Swagger (CRÍTICO para evitar 403)
                                "/webjars/**"                              // Recursos estáticos de librerías (CSS/JS de Swagger)
                        ).permitAll()

                        // ==========================================
                        // NIVEL 2: ENDPOINTS ESPECÍFICOS DE PROPIETARIO
                        // ⚠️ DEBEN IR ANTES de la regla general "/owners/**"
                        // ==========================================
                        .requestMatchers("/owners/accommodation/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/owners/reservations/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/owners/dashboard/**").hasAnyRole("OWNER", "ADMIN")

                        // ==========================================
                        // NIVEL 3: ENDPOINTS DE ADMINISTRACIÓN (solo ADMIN)
                        // Esta regla general captura cualquier otra ruta que empiece con /owners/ o /admin/
                        // ==========================================
                        .requestMatchers("/owners/**").hasRole("ADMIN")  // Gestión de dueños
                        .requestMatchers("/admin/**").hasRole("ADMIN")   // Métricas y config global

                        // ==========================================
                        // NIVEL 4: CUALQUIER OTRA PETICIÓN
                        // Requiere autenticación válida (cualquier rol)
                        // ==========================================
                        .anyRequest().authenticated()
                )

                // 4. Configurar gestión de sesiones (STATELESS = sin sesión HTTP, solo JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 5. Configurar proveedor de autenticación
                .authenticationProvider(authenticationProvider())

                // 6. Agregar el filtro JWT antes del filtro estándar de autenticación por username/password
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Configura el proveedor de autenticación que usa Spring Security.
     * Vincula el servicio de usuarios y el codificador de contraseñas.
     *
     * @return el proveedor de autenticación configurado
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        // Se usa la instancia inyectada directamente, sin llamar a un método
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Expone el {@link AuthenticationManager} como un bean de Spring.
     * Necesario para que el AuthService pueda autenticar las credenciales manualmente.
     *
     * @param authConfig la configuración de autenticación de Spring
     * @return el administrador de autenticación
     * @throws Exception si no se puede obtener el AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configura CORS (Cross-Origin Resource Sharing) para permitir
     * peticiones desde el frontend (u otros orígenes autorizados).
     *
     * @return la configuración de CORS aplicada a todas las rutas
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos (en desarrollo, permitir localhost)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",      // Frontend React/Vue/Angular
                "http://localhost:4200",
                "http://localhost:8080",      // Swagger UI
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080",
                "https://hostflow-deploy.vercel.app"
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Headers permitidos en las peticiones
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Headers expuestos al cliente en la respuesta
        configuration.setExposedHeaders(List.of(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Authorization"
        ));

        // Permitir credenciales (cookies, tokens de autorización)
        configuration.setAllowCredentials(true);

        // Tiempo máximo de caché para preflight requests (en segundos)
        configuration.setMaxAge(3600L);

        // Aplicar la configuración a todas las rutas de la aplicación
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
package com.hostflow.config;

import com.hostflow.security.CustomUserDetailsService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración central de seguridad para la aplicación HostFlow.
 *
 * <p>Esta clase configura todos los aspectos de seguridad de la aplicación, incluyendo:</p>
 * <ul>
 *   <li>La cadena de filtros de seguridad (SecurityFilterChain)</li>
 *   <li>La autenticación basada en JWT (JSON Web Tokens)</li>
 *   <li>La configuración CORS para permitir peticiones cruzadas</li>
 *   <li>Los permisos de acceso a los diferentes endpoints</li>
 *   <li>La gestión de sesiones stateless (sin estado)</li>
 * </ul>
 *
 * <p>Utiliza Spring Security 6+ con la nueva configuración basada en lambdas y
 * deshabilita CSRF ya que la API es stateless y utiliza JWT para autenticación.</p>
 *
 * @author HostFlow Team
 * @version 1.1
 * @see EnableWebSecurity
 * @see EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Filtro de autenticación JWT que intercepta cada petición para validar tokens.
     * Se inyecta automáticamente vía constructor gracias a @RequiredArgsConstructor.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Servicio personalizado para cargar detalles del usuario desde la base de datos.
     * Implementa la interfaz UserDetailsService de Spring Security.
     */
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Codificador de contraseñas BCrypt inyectado desde PasswordEncoderConfig.
     * Se utiliza para verificar las contraseñas hasheadas durante la autenticación.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Configura la cadena principal de filtros de seguridad.
     *
     * <p>Este método define el comportamiento de seguridad para todas las peticiones HTTP:
     * <ul>
     *   <li>Deshabilita CSRF (innecesario en APIs stateless con JWT)</li>
     *   <li>Configura CORS para permitir peticiones desde el frontend</li>
     *   <li>Define qué endpoints son públicos y cuáles requieren autenticación</li>
     *   <li>Establece la política de sesiones como STATELESS (sin estado)</li>
     *   <li>Registra el proveedor de autenticación y el filtro JWT</li>
     * </ul>
     * </p>
     *
     * @param http el objeto HttpSecurity que permite configurar la seguridad HTTP
     * @return SecurityFilterChain la cadena de filtros completamente configurada
     * @throws Exception si ocurre algún error durante la configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. DESHABILITAR CSRF
                // No es necesario en APIs REST que usan JWT y son stateless.
                // Previene ataques de falsificación de peticiones, pero interfiere con APIs sin cookies de sesión.
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CONFIGURAR CORS
                // Permite peticiones desde el frontend (React, Angular, etc.)
                // configurado en el método corsConfigurationSource() más abajo.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. AUTORIZACIÓN DE PETICIONES
                .authorizeHttpRequests(auth -> auth
                        // ==========================================
                        // ENDPOINTS PÚBLICOS (SIN AUTENTICACIÓN)
                        // ==========================================

                        // Endpoints de autenticación y registro.
                        // Se incluyen ambas rutas (/auth/** y /api/v1/auth/**) para cubrir
                        // cualquier variación en la definición del @RequestMapping del controlador.
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Documentación Swagger/OpenAPI (acceso público solo en desarrollo)
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/v3/api-docs.yaml").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/api-docs.yaml").permitAll()

                        // Recursos estáticos públicos
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/public/**").permitAll()

                        // ==========================================
                        // ENDPOINTS PROTEGIDOS (REQUIEREN AUTH)
                        // ==========================================
                        // Cualquier otra petición no listada arriba requiere autenticación válida (JWT)
                        .anyRequest().authenticated()
                )

                // 4. GESTIÓN DE SESIONES
                // STATELESS significa que no se crea sesión HTTP en el servidor.
                // Cada petición debe ser independiente y contener su propio token JWT.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 5. PROVEEDOR DE AUTENTICACIÓN
                // Configura cómo se autentican los usuarios (DAO + BCrypt)
                .authenticationProvider(authenticationProvider())

                // 6. FILTRO JWT
                // Se ejecuta antes del filtro estándar de autenticación por username/password
                // para interceptar y validar el token JWT del header "Authorization".
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura el proveedor de autenticación DAO (Data Access Object).
     *
     * <p>Este proveedor es responsable de:
     * <ul>
     *   <li>Cargar los datos del usuario desde la base de datos</li>
     *   <li>Verificar la contraseña usando BCrypt</li>
     *   <li>Validar las credenciales durante el proceso de login</li>
     * </ul>
     * </p>
     *
     * @return AuthenticationProvider configurado con UserDetailsService y PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        // Configurar el servicio que carga los usuarios desde la BD
        authProvider.setUserDetailsService(customUserDetailsService);

        // Configurar el codificador de contraseñas (BCrypt)
        // NOTA: Se usa el bean inyectado en el constructor, NO se llama a un método.
        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    /**
     * Expone el AuthenticationManager como bean para uso personalizado.
     *
     * <p>El AuthenticationManager es el punto de entrada principal para
     * la autenticación en Spring Security. Se puede inyectar en otros
     * componentes (como el servicio de login) cuando se necesita autenticar
     * programáticamente las credenciales del usuario.</p>
     *
     * @param authConfig la configuración de autenticación de Spring
     * @return AuthenticationManager el gestor de autenticación configurado
     * @throws Exception si no se puede obtener el AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configura las políticas CORS (Cross-Origin Resource Sharing).
     *
     * <p>Permite que el frontend (ejecutándose en diferentes puertos/dominios)
     * pueda hacer peticiones a la API backend. Esta configuración es esencial
     * durante el desarrollo cuando el frontend y backend corren en puertos
     * diferentes (ej: localhost:3000 → localhost:8080).</p>
     *
     * <p>Configuración aplicada:
     * <ul>
     *   <li>Orígenes permitidos: localhost en puertos comunes de desarrollo</li>
     *   <li>Métodos HTTP permitidos: GET, POST, PUT, PATCH, DELETE, OPTIONS</li>
     *   <li>Headers permitidos: todos (*)</li>
     *   <li>Credenciales: permitidas (cookies, headers de autorización)</li>
     *   <li>Max age: 1 hora (3600 segundos) para caché de peticiones preflight</li>
     * </ul>
     * </p>
     *
     * @return CorsConfigurationSource configuración CORS aplicada a todos los endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos (frontend)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",  // React/Next.js
                "http://localhost:4200",  // Angular
                "http://localhost:8080",   // Otro backend o frontend
                "http://localhost:5173"  // Vite
        ));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Headers permitidos (incluyendo Authorization para JWT)
        configuration.setAllowedHeaders(List.of("*"));

        // Permitir envío de credenciales (cookies, headers de auth)
        configuration.setAllowCredentials(true);

        // Tiempo de caché para respuestas preflight (en segundos)
        configuration.setMaxAge(3600L);

        // Aplicar configuración a todos los endpoints de la aplicación
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
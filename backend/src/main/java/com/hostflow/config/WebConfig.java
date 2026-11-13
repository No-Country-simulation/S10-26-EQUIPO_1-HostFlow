package com.hostflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing) para HostFlow a nivel de Spring MVC.
 *
 * <p>Permite que el frontend, alojado en dominios diferentes al backend,
 * realice peticiones HTTP a la API de manera segura y controlada.</p>
 *
 * <p><strong>⚠️ Nota de Arquitectura Crítica:</strong></p>
 * <p>Esta configuración actúa a nivel del Dispatcher Servlet de Spring MVC.
 * Dado que la aplicación utiliza Spring Security, es <strong>imperativo</strong> que
 * los mismos orígenes estén configurados en el {@code CorsConfigurationSource} de
 * {@link SecurityConfig}, ya que los filtros de seguridad se ejecutan <em>antes</em>
 * que esta configuración. Si no coinciden, las peticiones del navegador serán bloqueadas.</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configura las reglas de CORS para todos los endpoints de la aplicación.
     *
     * @param registry el registro de configuraciones CORS de Spring MVC
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // ✅ 1. Orígenes permitidos: entornos de desarrollo local y producción en Vercel
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:4200",
                        "http://localhost:8080",
                        "https://hostflow-deploy.vercel.app" // 🚀 Frontend desplegado en producción
                )
                // ✅ 2. Métodos HTTP permitidos (incluye OPTIONS para las peticiones "preflight")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // ✅ 3. Cabeceras que el cliente está autorizado a enviar en la petición
                .allowedHeaders("*")
                // ✅ 4. Cabeceras que el servidor permite que el cliente lea en la respuesta
                // (ej. para leer el token de autorización o la disposición de archivos)
                .exposedHeaders("Authorization", "Content-Disposition")
                // ✅ 5. Permite el envío de credenciales (cookies, headers de autorización como Bearer)
                // Nota: Esto requiere que 'allowedOrigins' NO sea "*".
                .allowCredentials(true)
                // ✅ 6. Tiempo en segundos que el navegador puede cachear la respuesta "preflight" (OPTIONS).
                // 3600 segundos = 1 hora, lo que reduce la carga de peticiones OPTIONS repetitivas.
                .maxAge(3600);
    }
}
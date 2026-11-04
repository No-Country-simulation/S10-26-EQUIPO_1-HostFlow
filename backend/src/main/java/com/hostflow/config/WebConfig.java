package com.hostflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing) para HostFlow.
 *
 * <p>Permite que el frontend (que puede estar en un dominio diferente)
 * realice peticiones a la API backend. En producción, se debe restringir
 * el origen permitido a los dominios específicos del frontend.</p>
 *
 * @author HostFlow Team
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configura las reglas de CORS para la aplicación.
     *
     * @param registry el registro de configuraciones CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // En producción, reemplazar "*" por los dominios específicos del frontend
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600); // Cache de preflight por 1 hora
    }
}
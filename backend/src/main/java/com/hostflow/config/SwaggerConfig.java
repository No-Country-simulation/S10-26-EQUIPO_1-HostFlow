package com.hostflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de Swagger / OpenAPI 3 para la documentación de la API de HostFlow.
 *
 * <p>Define la información general de la API, los esquemas de seguridad
 * (JWT Bearer Token), los metadatos de contacto y la URL base del servidor.</p>
 *
 * <p><strong>Ubicación de la documentación:</strong></p>
 * <ul>
 *   <li>Interfaz gráfica (Swagger UI): {@code http://localhost:8080/api/v1/swagger-ui.html}</li>
 *   <li>Definición OpenAPI (JSON): {@code http://localhost:8080/api/v1/v3/api-docs}</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configura el bean de OpenAPI con la información de la API,
     * la URL del servidor y el esquema de seguridad JWT Bearer.
     *
     * @return la configuración completa de OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()

                // =========================================================================
                // 1. INFORMACIÓN GENERAL DE LA API
                // =========================================================================
                .info(new Info()
                        .title("HostFlow API")
                        .version("1.0.0")
                        .description("""
                                API REST para el sistema de gestión integral de alojamientos turísticos HostFlow.
                                
                                Permite el registro y gestión de propietarios, administración de alojamientos, 
                                reservas, huéspedes y procesamiento de pagos multi-moneda.
                                
                                ## 🔐 Autenticación
                                La API utiliza tokens JWT (JSON Web Token) para la autenticación.
                                1. Use el endpoint `/auth/login` para obtener su token.
                                2. Haga clic en el botón **"Authorize"** (candado) en la parte superior.
                                3. Ingrese el token con el formato: `Bearer <tu_token_aquí>`
                                """)
                        .contact(new Contact()
                                .name("HostFlow Team")
                                .email("support@hostflow.com")
                                .url("https://hostflow.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                )

                // =========================================================================
                // 2. CONFIGURACIÓN DEL SERVIDOR (CRÍTICO PARA CONTEXTO /api/v1)
                // =========================================================================
                // Define explícitamente la URL base. Esto previene el error
                // "Failed to load remote configuration" al indicar a Swagger
                // que debe buscar los docs en /api/v1/v3/api-docs y no en la raíz.
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api/v1")
                                .description("Servidor de Desarrollo Local")
                ))

                // =========================================================================
                // 3. ESQUEMA DE SEGURIDAD (COMPONENTE REUTILIZABLE)
                // =========================================================================
                // Define el esquema "Bearer Authentication" como disponible.
                // No se aplica globalmente a todos los endpoints aquí; cada
                // controlador debe declarar explícitamente:
                // @SecurityRequirement(name = "Bearer Authentication")
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT. Ejemplo: Bearer eyJhbGciOiJIUzI1Ni...")
                        )
                );
    }
}
package com.hostflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger / OpenAPI 3 para la documentación de la API.
 *
 * <p>Define la información general de la API, los esquemas de seguridad
 * (JWT Bearer Token) y los metadatos de contacto. La documentación
 * estará disponible en:</p>
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui.html}</li>
 *   <li>OpenAPI JSON: {@code /api-docs}</li>
 * </ul>
 *
 * @author HostFlow Team
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configura el bean de OpenAPI con la información de la API
     * y el esquema de seguridad JWT Bearer.
     *
     * @return la configuración de OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Información general de la API
                .info(new Info()
                        .title("HostFlow API")
                        .version("1.0.0")
                        .description("""
                                API REST para el sistema de gestión de alojamientos HostFlow.
                                
                                Permite el registro y gestión de dueños de alojamientos,
                                autenticación mediante JWT y administración de recursos.
                                
                                ## Autenticación
                                La API utiliza tokens JWT (JSON Web Token) para la autenticación.
                                Para autenticarse, use el endpoint `/auth/login` y copie el token
                                retornado. Luego, haga clic en el botón "Authorize" e ingrese
                                el token con el formato: `Bearer <tu_token>`
                                """)
                        .contact(new Contact()
                                .name("HostFlow Team")
                                .email("support@hostflow.com")
                                .url("https://hostflow.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                )
                // Definir el esquema de seguridad JWT Bearer como DISPONIBLE
// pero NO obligatorio globalmente.
// Cada controlador que requiera autenticación debe declarar
// explícitamente @SecurityRequirement(name = "Bearer Authentication")
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .bearerFormat("JWT")
                                        .scheme("bearer")
                                        .description("Ingrese el token JWT con el formato: Bearer <token>")
                        )
                );
    }
}
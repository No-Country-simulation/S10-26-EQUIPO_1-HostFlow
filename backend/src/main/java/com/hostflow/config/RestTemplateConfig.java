package com.hostflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración del bean RestTemplate utilizado para consumir APIs externas.
 *
 * <p>Se utiliza en {@code ExchangeRateProviderService} para consultar
 * las APIs de tasas de cambio (DolarAPI.com y Frankfurter API).</p>
 *
 * <p>Se configuran timeouts razonables para evitar que la aplicación
 * quede bloqueada si una API externa no responde.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Bean de RestTemplate con timeouts configurados.
     *
     * <ul>
     *   <li>Connect timeout: 5 segundos (tiempo máximo para establecer conexión).</li>
     *   <li>Read timeout: 10 segundos (tiempo máximo para recibir respuesta).</li>
     * </ul>
     *
     * @return RestTemplate configurado con timeouts
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Tiempo máximo para establecer la conexión (5 segundos)
        factory.setConnectTimeout(5000);
        // Tiempo máximo para recibir la respuesta completa (10 segundos)
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
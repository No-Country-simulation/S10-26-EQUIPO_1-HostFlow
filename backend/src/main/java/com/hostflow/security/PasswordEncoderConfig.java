package com.hostflow.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración del codificador de contraseñas.
 *
 * <p>Utiliza {@link BCryptPasswordEncoder} que implementa el algoritmo
 * BCrypt, un función de hashing adaptativa diseñada específicamente
 * para contraseñas. BCrypt incluye un factor de trabajo (cost factor)
 * que hace que el hashing sea deliberadamente lento, protegiendo
 * contra ataques de fuerza bruta.</p>
 *
 * @author HostFlow Team
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Bean del codificador de contraseñas BCrypt.
     *
     * <p>Se registra como un bean de Spring para que pueda ser
     * inyectado en cualquier servicio que necesite encriptar
     * o verificar contraseñas.</p>
     *
     * @return una instancia de {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
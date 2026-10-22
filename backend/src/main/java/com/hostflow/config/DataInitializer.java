package com.hostflow.config;

import com.hostflow.enums.RoleType;
import com.hostflow.model.Owner;
import com.hostflow.model.Role;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Inicializador de datos base (Seed Data) para el entorno de desarrollo de HostFlow.
 *
 * <p>Este componente implementa {@link CommandLineRunner} para ejecutarse automáticamente
 * una vez que el contexto de la aplicación de Spring Boot se ha inicializado por completo.
 * Su responsabilidad es garantizar la existencia de los roles base del sistema y de un
 * super usuario administrador, asegurando que la plataforma sea funcional desde el primer arranque.</p>
 *
 * <p><strong>Características de diseño:</strong></p>
 * <ul>
 *   <li><strong>Idempotencia:</strong> Las operaciones verifican la existencia previa de los datos
 *   antes de intentar crearlos, permitiendo ejecutar la aplicación múltiples veces sin generar
 *   errores de duplicidad (ConstraintViolationException).</li>
 *   <li><strong>Seguridad:</strong> Las contraseñas se procesan mediante {@link PasswordEncoder}
 *   (BCrypt) antes de persistirse, nunca en texto plano.</li>
 *   <li><strong>Resiliencia:</strong> El bloque try-catch evita que un fallo en la carga de datos
 *   iniciales bloquee el arranque completo de la aplicación.</li>
 * </ul>
 *
 * <p><strong>⚠️ ADVERTENCIA DE SEGURIDAD:</strong> Este inicializador está diseñado exclusivamente
 * para entornos de desarrollo (perfil 'dev'). En producción, debe ser deshabilitado mediante
 * perfiles de Spring (@Profile("dev")) o eliminado del contexto para evitar la exposición
 * de credenciales por defecto.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
@DependsOn("entityManagerFactory") // CLAVE: Fuerza a Spring a esperar a que Hibernate termine de crear las tablas antes de ejecutar este runner.
public class DataInitializer implements CommandLineRunner {

    /**
     * Repositorio para operaciones de persistencia de la entidad {@link Role}.
     */
    private final RoleRepository roleRepository;

    /**
     * Repositorio para operaciones de persistencia de la entidad {@link Owner}.
     */
    private final OwnerRepository ownerRepository;

    /**
     * Implementación de codificación de contraseñas inyectada por Spring Security.
     * Garantiza el cumplimiento de las prácticas de seguridad de almacenamiento de credenciales.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Método principal del ciclo de vida de {@link CommandLineRunner}.
     * Se ejecuta después de que todos los beans y la base de datos estén listos.
     *
     * @param args argumentos de línea de comandos pasados a la aplicación (no utilizados aquí).
     */
    @Override
    @Transactional // Garantiza atomicidad: si falla la creación del admin, se revierte cualquier cambio parcial en esta transacción.
    public void run(String... args) {
        log.info("🚀 Iniciando proceso de carga de datos base (Seed Data) del sistema HostFlow...");

        try {
            // Paso 1: Asegurar la existencia de los roles del sistema
            inicializarRoles();

            // Paso 2: Asegurar la existencia del usuario administrador
            inicializarAdministrador();

            log.info("✅ Proceso de inicialización de datos base completado exitosamente.");
        } catch (Exception e) {
            // Capturamos la excepción para evitar que un error en los datos iniciales
            // derribe toda la aplicación. La app arrancará, pero con un registro del fallo.
            log.error("❌ Error crítico durante la inicialización de datos base: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica y crea los roles del sistema de manera idempotente.
     *
     * <p>Utiliza {@code orElseGet} en lugar de {@code orElse} para evaluar
     * perezosamente (lazy evaluation) la creación del rol, evitando consultas
     * de escritura (INSERT) innecesarias si el rol ya existe en la base de datos.</p>
     */
    private void inicializarRoles() {
        log.info("📋 Verificando integridad de roles del sistema...");

        roleRepository.findByRoleName(RoleType.ROLE_ADMIN).orElseGet(() -> {
            log.info("   ➕ Rol no encontrado. Creando: ROLE_ADMIN");
            return roleRepository.save(new Role(RoleType.ROLE_ADMIN));
        });

        roleRepository.findByRoleName(RoleType.ROLE_OWNER).orElseGet(() -> {
            log.info("   ➕ Rol no encontrado. Creando: ROLE_OWNER");
            return roleRepository.save(new Role(RoleType.ROLE_OWNER));
        });
    }

    /**
     * Verifica y crea el super usuario administrador del sistema.
     *
     * <p>Asigna tanto el rol de ADMIN como el de OWNER al administrador,
     * garantizando que tenga visibilidad total del sistema y pueda actuar
     * como un dueño de alojamiento estándar cuando sea necesario.</p>
     */
    private void inicializarAdministrador() {
        final String adminEmail = "adminhostflow@gmail.com";

        // Verificación eficiente sin traer toda la entidad a memoria
        if (!ownerRepository.existsByEmail(adminEmail)) {
            log.info("👤 Usuario administrador no encontrado. Procediendo a su creación...");

            // Recuperamos los roles. Si no existen aquí, algo falló catastróficamente en el paso anterior.
            Role adminRole = roleRepository.findByRoleName(RoleType.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Inconsistencia de datos: El rol ROLE_ADMIN debería existir en este punto."));

            Role ownerRole = roleRepository.findByRoleName(RoleType.ROLE_OWNER)
                    .orElseThrow(() -> new IllegalStateException("Inconsistencia de datos: El rol ROLE_OWNER debería existir en este punto."));

            // Construcción de la entidad utilizando el patrón Builder para inmutabilidad y legibilidad
            Owner admin = Owner.builder()
                    .fullName("Administrador HostFlow")
                    .accommodationName("HostFlow Platform")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("adminPassword")) // Encriptación BCrypt obligatoria
                    .termsAccepted(true)
                    .enabled(true)
                    .roles(Set.of(adminRole, ownerRole))
                    .build();

            ownerRepository.save(admin);

            log.info("   ✅ Super usuario creado y persistido exitosamente.");
            log.info("      📧 Email:    {}", adminEmail);
            log.info("      ⚠️  NOTA DE SEGURIDAD: Cambie estas credenciales inmediatamente antes de pasar a producción.");
        } else {
            log.info("   ℹ️  El super usuario administrador ya existe. Omitiendo creación para preservar integridad.");
        }
    }
}
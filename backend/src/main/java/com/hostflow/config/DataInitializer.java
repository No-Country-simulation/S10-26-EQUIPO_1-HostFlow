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
 * para entornos de desarrollo. En producción, debe ser deshabilitado mediante perfiles de Spring
 * (@Profile("dev")) o eliminado del contexto para evitar la exposición de credenciales por defecto.</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
@DependsOn("entityManagerFactory") // Fuerza a Spring a esperar a que Hibernate cree las tablas antes de ejecutar este runner.
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;

    // =========================================================================
    // CREDENCIALES DEL ADMINISTRADOR POR DEFECTO (Solo para Desarrollo)
    // =========================================================================
    private static final String ADMIN_EMAIL = "adminhostflow@gmail.com";
    private static final String ADMIN_PASSWORD = "AdminHostFlow2026!";
    private static final String ADMIN_FULL_NAME = "Administrador HostFlow";
    private static final String ADMIN_ACCOMMODATION = "HostFlow Platform";

    @Override
    @Transactional // Garantiza atomicidad: si falla la creación, se revierte cualquier cambio parcial.
    public void run(String... args) {
        log.info("🚀 Iniciando proceso de carga de datos base (Seed Data) del sistema HostFlow...");

        try {
            inicializarRoles();
            inicializarAdministrador();
            log.info("✅ Proceso de inicialización de datos base completado exitosamente.");
        } catch (Exception e) {
            log.error("❌ Error crítico durante la inicialización de datos base: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica y crea los roles del sistema de manera idempotente.
     *
     * <p>Utiliza {@code orElseGet} para evaluación perezosa (lazy evaluation),
     * evitando consultas de escritura (INSERT) innecesarias si el rol ya existe.</p>
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
        // Verificación eficiente sin traer toda la entidad a memoria
        if (!ownerRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("👤 Usuario administrador no encontrado. Procediendo a su creación...");

            // Recuperamos los roles. Si no existen aquí, algo falló en el paso anterior.
            Role adminRole = roleRepository.findByRoleName(RoleType.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Inconsistencia de datos: El rol ROLE_ADMIN debería existir."));

            Role ownerRole = roleRepository.findByRoleName(RoleType.ROLE_OWNER)
                    .orElseThrow(() -> new IllegalStateException("Inconsistencia de datos: El rol ROLE_OWNER debería existir."));

            // Construcción de la entidad utilizando el patrón Builder
            Owner admin = Owner.builder()
                    .fullName(ADMIN_FULL_NAME)
                    .accommodationName(ADMIN_ACCOMMODATION)
                    .email(ADMIN_EMAIL)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD)) // Encriptación BCrypt obligatoria
                    .termsAccepted(true)
                    .enabled(true)
                    .emailVerified(true) // ✅ CORRECCIÓN: El admin del sistema debe estar verificado automáticamente
                    .roles(Set.of(adminRole, ownerRole)) // ✅ CLAVE: Se le asignan AMBOS roles
                    .build();

            ownerRepository.save(admin);

            log.info("   ✅ Super usuario creado y persistido exitosamente.");
            log.info("      📧 Email:    {}", ADMIN_EMAIL);
            log.info("      🔑 Password: {}", ADMIN_PASSWORD);
            log.info("      🎭 Roles:    ROLE_ADMIN, ROLE_OWNER");
            log.info("      ⚠️  NOTA: Si intentaste registrar este correo manualmente antes, este script lo omite. Borra el registro manual de la BD si quieres que este script tome el control.");
        } else {
            log.info("   ℹ️  El super usuario administrador ya existe. Omitiendo creación para preservar integridad.");
            log.info("   💡 TIP: Si este usuario tiene el rol incorrecto (solo ROLE_OWNER), bórralo de la tabla 'owners' y reinicia la aplicación.");
        }
    }
}
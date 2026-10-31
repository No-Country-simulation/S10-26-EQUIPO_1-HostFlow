package com.hostflow.security;

import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

/**
 * Implementación personalizada de {@link UserDetailsService} para HostFlow.
 *
 * <p>Este servicio es utilizado por Spring Security para cargar los detalles
 * del usuario durante el proceso de autenticación. Busca al usuario por
 * su correo electrónico en la base de datos y construye un objeto
 * {@link UserDetails} con sus credenciales y roles.</p>
 *
 * @author HostFlow Team
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Repositorio para acceder a los datos de los dueños.
     */
    private final OwnerRepository ownerRepository;

    /**
     * Carga los detalles del usuario por su correo electrónico.
     *
     * <p>Este método es invocado automáticamente por Spring Security
     * durante el proceso de autenticación cuando el usuario intenta
     * iniciar sesión.</p>
     *
     * @param email el correo electrónico del usuario a buscar
     * @return los detalles del usuario incluyendo credenciales y roles
     * @throws UsernameNotFoundException si no se encuentra un usuario con ese email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Buscar el dueño por correo electrónico
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No se encontró un usuario con el correo: " + email));

        // Convertir los roles de la entidad a autoridades de Spring Security
        var authorities = owner.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());

        // Construir y retornar el objeto UserDetails
        return User.builder()
                .username(owner.getEmail())
                .password(owner.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!owner.getEnabled())
                .build();
    }
}
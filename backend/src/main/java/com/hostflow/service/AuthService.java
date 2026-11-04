package com.hostflow.service;


import com.hostflow.dto.request.LoginRequest;
import com.hostflow.dto.request.OwnerRegisterRequest;
import com.hostflow.dto.response.AuthResponse;
import com.hostflow.dto.response.OwnerResponse;
import com.hostflow.enums.RoleType;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.Owner;
import com.hostflow.model.Role;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Servicio de autenticación y registro de usuarios para HostFlow.
 *
 * <p>Maneja el flujo completo de autenticación:</p>
 * <ul>
 *   <li>Registro de nuevos dueños de alojamiento</li>
 *   <li>Inicio de sesión con generación de tokens JWT</li>
 * </ul>
 *
 * <p>Este servicio coordina la interacción entre el repositorio de datos,
 * el codificador de contraseñas, el servicio JWT y el manager de autenticación.</p>
 *
 * @author HostFlow Team
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Repositorio para acceder a los datos de los dueños.
     */
    private final OwnerRepository ownerRepository;

    /**
     * Repositorio para acceder a los roles del sistema.
     */
    private final RoleRepository roleRepository;

    /**
     * Codificador de contraseñas BCrypt.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Manager de autenticación de Spring Security.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Servicio para operaciones con tokens JWT.
     */
    private final JwtService jwtService;

    /**
     * Registra un nuevo dueño de alojamiento en el sistema.
     *
     * <p>Flujo del registro:</p>
     * <ol>
     *   <li>Valida que el correo electrónico no esté registrado.</li>
     *   <li>Valida que se hayan aceptado los términos y condiciones.</li>
     *   <li>Encripta la contraseña con BCrypt.</li>
     *   <li>Asigna el rol {@code ROLE_OWNER} al nuevo dueño.</li>
     *   <li>Persiste el dueño en la base de datos.</li>
     *   <li>Genera un token JWT para el nuevo usuario.</li>
     *   <li>Retorna la respuesta de autenticación con el token.</li>
     * </ol>
     *
     * @param request los datos de registro del dueño
     * @return la respuesta de autenticación con el token JWT
     * @throws BusinessException si el correo ya está registrado o no se aceptaron los términos
     */
    @Transactional
    public AuthResponse registerOwner(OwnerRegisterRequest request) {

        // Validar que se hayan aceptado los términos y condiciones
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new BusinessException(
                    "Debe aceptar los términos y la política de privacidad para registrarse");
        }

        // Validar que el correo electrónico no esté ya registrado
        if (ownerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    "Ya existe una cuenta registrada con el correo: " + request.getEmail());
        }

        // Buscar o crear el rol ROLE_OWNER
        Role ownerRole = roleRepository.findByRoleName(RoleType.ROLE_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(RoleType.ROLE_OWNER)));

        // Construir la entidad Owner con los datos del registro
        Owner owner = Owner.builder()
                .fullName(request.getFullName())
                .accommodationName(request.getAccommodationName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .termsAccepted(request.getTermsAccepted())
                .enabled(true)
                .roles(Set.of(ownerRole))
                .build();

        // Persistir el nuevo dueño en la base de datos
        Owner savedOwner = ownerRepository.save(owner);

        // Generar el token JWT para el nuevo usuario
        String token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        savedOwner.getEmail(),
                        savedOwner.getPassword(),
                        savedOwner.getRoles().stream()
                                .map(role -> new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority(role.getRoleName().name()))
                                .collect(java.util.stream.Collectors.toList())
                )
        );

        // Construir y retornar la respuesta de autenticación
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedOwner.getId())
                .email(savedOwner.getEmail())
                .fullName(savedOwner.getFullName())
                .roles(savedOwner.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }

    /**
     * Autentica un usuario existente y genera un token JWT.
     *
     * <p>Flujo de autenticación:</p>
     * <ol>
     *   <li>Intenta autenticar las credenciales con el AuthenticationManager.</li>
     *   <li>Si la autenticación es exitosa, busca al usuario en la base de datos.</li>
     *   <li>Genera un token JWT con la información del usuario.</li>
     *   <li>Retorna la respuesta de autenticación con el token.</li>
     * </ol>
     *
     * @param request las credenciales del usuario (email y contraseña)
     * @return la respuesta de autenticación con el token JWT
     * @throws BusinessException si las credenciales son incorrectas
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        // Intentar autenticar al usuario con las credenciales proporcionadas
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Buscar al usuario en la base de datos
        Owner owner = ownerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "No se encontró un usuario con el correo: " + request.getEmail()));

        // Generar el token JWT
        String token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        owner.getEmail(),
                        owner.getPassword(),
                        owner.getRoles().stream()
                                .map(role -> new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority(role.getRoleName().name()))
                                .collect(java.util.stream.Collectors.toList())
                )
        );

        // Construir y retornar la respuesta de autenticación
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(owner.getId())
                .email(owner.getEmail())
                .fullName(owner.getFullName())
                .roles(owner.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }
}
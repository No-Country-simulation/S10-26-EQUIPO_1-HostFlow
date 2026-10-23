package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationPaymentMethodRequest;
import com.hostflow.dto.response.AccommodationPaymentMethodResponse;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.AccommodationPaymentMethod;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.service.AccommodationPaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/owners/accommodation/payment-methods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Métodos de pago del alojamiento", description = "Gestión de múltiples métodos de pago del alojamiento propio")
public class AccommodationPaymentMethodController {
    private final AccommodationPaymentMethodService service;
    private final OwnerRepository ownerRepository;

    @GetMapping
    @Operation(summary = "Listar mis métodos de pago", description = "Solo devuelve métodos pertenecientes al alojamiento del propietario autenticado.")
    public ResponseEntity<List<AccommodationPaymentMethodResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(service.list(currentOwnerId(authentication)).stream()
                .map(AccommodationPaymentMethodResponse::from).toList());
    }

    @PostMapping
    @Operation(summary = "Registrar un método de pago", description = "paymentType y displayName son obligatorios. Si no existe un método predeterminado, el primero se establece como tal.")
    public ResponseEntity<AccommodationPaymentMethodResponse> create(
            @Valid @RequestBody AccommodationPaymentMethodRequest request, Authentication authentication) {
        AccommodationPaymentMethod created = service.create(currentOwnerId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccommodationPaymentMethodResponse.from(created));
    }

    @PatchMapping("/{methodId}")
    @Operation(summary = "Actualizar parcialmente un método de pago", description = "Solo modifica los campos enviados y únicamente si el método pertenece al alojamiento autenticado.")
    public ResponseEntity<AccommodationPaymentMethodResponse> update(
            @PathVariable Long methodId, @Valid @RequestBody AccommodationPaymentMethodRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(AccommodationPaymentMethodResponse.from(
                service.update(currentOwnerId(authentication), methodId, request)));
    }

    @DeleteMapping("/{methodId}")
    @Operation(summary = "Eliminar un método de pago", description = "Elimina solo métodos del alojamiento propio. Si se elimina el predeterminado, se elige otro existente automáticamente.")
    public ResponseEntity<Void> delete(@PathVariable Long methodId, Authentication authentication) {
        service.delete(currentOwnerId(authentication), methodId);
        return ResponseEntity.noContent().build();
    }

    private Long currentOwnerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Se requiere autenticación.");
        }
        Owner owner = ownerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el propietario autenticado."));
        return owner.getId();
    }
}

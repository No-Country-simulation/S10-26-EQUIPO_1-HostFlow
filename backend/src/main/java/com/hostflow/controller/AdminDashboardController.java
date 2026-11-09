package com.hostflow.controller;

import com.hostflow.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Dashboard Admin", description = "Métricas globales de la plataforma HostFlow")
public class AdminDashboardController {

    // ✅ SOLO inyecta el servicio, NO los repositorios
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/metrics")
    @Operation(summary = "Obtener métricas globales del administrador")
    public ResponseEntity<Map<String, Object>> getAdminMetrics() {
        // ✅ Solo delega al servicio
        return ResponseEntity.ok(adminDashboardService.getAdminMetrics());
    }
}
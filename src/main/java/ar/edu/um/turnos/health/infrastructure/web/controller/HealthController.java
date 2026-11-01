package ar.edu.um.turnos.health.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.health.application.service.HealthService;
import ar.edu.um.turnos.health.infrastructure.web.dto.EstadoServicioResponse;
import ar.edu.um.turnos.health.infrastructure.web.mapper.EstadoServicioWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de entrada HTTP. Depende de la fachada del slice y del mapper web,
 * nunca de un caso de uso ni de un puerto de salida.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Estado del servicio")
public class HealthController {

    private final HealthService healthService;
    private final EstadoServicioWebMapper mapper;

    @GetMapping
    @Operation(summary = "Consultar el estado del servicio")
    public ResponseEntity<EstadoServicioResponse> consultarEstado() {
        return ResponseEntity.ok(mapper.toResponse(healthService.consultarEstado()));
    }

}

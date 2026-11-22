package ar.edu.um.turnos.health.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.health.application.service.HealthService;
import ar.edu.um.turnos.health.domain.model.EstadoServicio;
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

    /**
     * {@code DOWN} (base de datos caida, #40) responde 503: es la unica
     * forma en la que un healthcheck que solo mira el codigo HTTP (como el de
     * {@code compose.yaml}) distingue esto de {@code UP} o {@code DEGRADADO},
     * que siguen respondiendo 200 porque el proceso atiende HTTP igual.
     */
    @GetMapping
    @Operation(summary = "Consultar el estado del servicio")
    public ResponseEntity<EstadoServicioResponse> consultarEstado() {
        EstadoServicio estadoServicio = healthService.consultarEstado();
        HttpStatus status = estadoServicio.getEstado() == EstadoServicio.Estado.DOWN
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(mapper.toResponse(estadoServicio));
    }

}

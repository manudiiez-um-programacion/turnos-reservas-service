package ar.edu.um.turnos.health.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.turnos.health.domain.model.EstadoServicio;
import ar.edu.um.turnos.health.domain.ports.in.ConsultarEstadoUseCase;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del slice: compone los casos de uso y es lo unico que la capa web
 * conoce. El controlador no habla con un caso de uso ni con un puerto de salida
 * directamente.
 */
@Service
@RequiredArgsConstructor
public class HealthService {

    private final ConsultarEstadoUseCase consultarEstadoUseCase;

    public EstadoServicio consultarEstado() {
        return consultarEstadoUseCase.consultar();
    }

}

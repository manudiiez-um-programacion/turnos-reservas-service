package ar.edu.um.turnos.health.application.usecases;

import java.time.Instant;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.health.domain.model.EstadoServicio;
import ar.edu.um.turnos.health.domain.ports.in.ConsultarEstadoUseCase;
import ar.edu.um.turnos.health.domain.ports.out.InformacionDelServicioPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementacion del caso de uso: orquesta el dominio y depende unicamente de
 * puertos, nunca de una clase de {@code infrastructure}.
 *
 * <p>Inyeccion por constructor via {@code @RequiredArgsConstructor}, nunca
 * {@code @Autowired} sobre campos.
 */
@Component
@RequiredArgsConstructor
public class ConsultarEstadoUseCaseImpl implements ConsultarEstadoUseCase {

    private final InformacionDelServicioPort informacionDelServicio;

    @Override
    public EstadoServicio consultar() {
        return EstadoServicio.builder()
                .nombre(informacionDelServicio.nombre())
                .version(informacionDelServicio.version())
                .estado(EstadoServicio.Estado.UP)
                .consultadoEn(Instant.now())
                .build();
    }

}

package ar.edu.um.turnos.health.application.usecases;

import java.time.Instant;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.health.domain.model.EstadoDependencia;
import ar.edu.um.turnos.health.domain.model.EstadoDependencias;
import ar.edu.um.turnos.health.domain.model.EstadoServicio;
import ar.edu.um.turnos.health.domain.ports.in.ConsultarEstadoUseCase;
import ar.edu.um.turnos.health.domain.ports.out.EstadoDependenciasPort;
import ar.edu.um.turnos.health.domain.ports.out.InformacionDelServicioPort;
import lombok.RequiredArgsConstructor;

/**
 * Implementacion del caso de uso: orquesta el dominio y depende unicamente de
 * puertos, nunca de una clase de {@code infrastructure}.
 *
 * <p>Inyeccion por constructor via {@code @RequiredArgsConstructor}, nunca
 * {@code @Autowired} sobre campos.
 *
 * <p>El estado agregado (#40) sigue la regla de §3: la base de datos caida es
 * {@code DOWN} (el servicio no puede operar en absoluto sin ella); la catedra
 * o el catalogo caidos, con la base arriba, son {@code DEGRADADO} (las
 * operaciones que dependen solo de datos propios siguen atendiendose, las que
 * necesitan validar informacion vigente no pueden iniciarse). Un estado
 * {@code DESCONOCIDO} (todavia no hubo ninguna llamada real) no degrada nada.
 */
@Component
@RequiredArgsConstructor
public class ConsultarEstadoUseCaseImpl implements ConsultarEstadoUseCase {

    private final InformacionDelServicioPort informacionDelServicio;
    private final EstadoDependenciasPort estadoDependenciasPort;

    @Override
    public EstadoServicio consultar() {
        EstadoDependencias dependencias = estadoDependenciasPort.consultar();
        return EstadoServicio.builder()
                .nombre(informacionDelServicio.nombre())
                .version(informacionDelServicio.version())
                .estado(calcularEstado(dependencias))
                .consultadoEn(Instant.now())
                .dependencias(dependencias)
                .build();
    }

    private EstadoServicio.Estado calcularEstado(EstadoDependencias dependencias) {
        if (dependencias.getBaseDeDatos() != EstadoDependencia.DISPONIBLE) {
            return EstadoServicio.Estado.DOWN;
        }
        if (dependencias.getCatedra() == EstadoDependencia.DEGRADADO
                || dependencias.getCatalogo() == EstadoDependencia.DEGRADADO) {
            return EstadoServicio.Estado.DEGRADADO;
        }
        return EstadoServicio.Estado.UP;
    }

}

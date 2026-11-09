package ar.edu.um.turnos.health.infrastructure.config.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.config.observabilidad.EstadoIntegracion;
import ar.edu.um.turnos.config.observabilidad.Integracion;
import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;
import ar.edu.um.turnos.health.domain.model.EstadoDependencia;
import ar.edu.um.turnos.health.domain.model.EstadoDependencias;
import ar.edu.um.turnos.health.domain.ports.out.EstadoDependenciasPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adaptador del puerto de salida (#40). La base de datos se verifica activa:
 * es local y de bajo costo, y {@code spring.datasource.hikari.connection-timeout}
 * la acota para que una base caida no cuelgue la respuesta de salud. La
 * catedra y el catalogo se leen de {@link MetricasObservabilidad}, el ultimo
 * estado conocido segun la llamada real mas reciente que ya hizo
 * {@code CatedraClient} o {@code CatalogoClient}: no se genera una llamada
 * nueva solo para contestar esta consulta (§3).
 *
 * <p>No se registra el detalle de una {@link SQLException} (mensaje, host,
 * credenciales que pudiera traer la causa): el endpoint de salud es publico
 * y no expone detalles internos ni secretos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstadoDependenciasAdapter implements EstadoDependenciasPort {

    private static final int TIMEOUT_VALIDACION_SEGUNDOS = 2;

    private final DataSource dataSource;
    private final MetricasObservabilidad metricas;

    @Override
    public EstadoDependencias consultar() {
        return EstadoDependencias.builder()
                .baseDeDatos(verificarBaseDeDatos())
                .catedra(traducir(metricas.estadoDe(Integracion.CATEDRA)))
                .catalogo(traducir(metricas.estadoDe(Integracion.CATALOGO)))
                .build();
    }

    private EstadoDependencia verificarBaseDeDatos() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(TIMEOUT_VALIDACION_SEGUNDOS) ? EstadoDependencia.DISPONIBLE : EstadoDependencia.DEGRADADO;
        } catch (SQLException fallo) {
            log.warn("La base de datos no respondio el chequeo de salud: {}", fallo.getClass().getSimpleName());
            return EstadoDependencia.DEGRADADO;
        }
    }

    private EstadoDependencia traducir(EstadoIntegracion estado) {
        if (!estado.esConocido()) {
            return EstadoDependencia.DESCONOCIDO;
        }
        return estado.disponible() ? EstadoDependencia.DISPONIBLE : EstadoDependencia.DEGRADADO;
    }

}

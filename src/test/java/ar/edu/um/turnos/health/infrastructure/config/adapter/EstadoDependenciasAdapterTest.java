package ar.edu.um.turnos.health.infrastructure.config.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.config.observabilidad.Integracion;
import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;
import ar.edu.um.turnos.health.domain.model.EstadoDependencia;
import ar.edu.um.turnos.health.domain.model.EstadoDependencias;

/**
 * Cubre el adaptador (#40): la base de datos se mockea a nivel JDBC (no es
 * una base real, es el mismo mecanismo que usan los adaptadores de
 * persistencia del proyecto), y la catedra/catalogo se alimentan a traves de
 * {@link MetricasObservabilidad}, tal como lo hace {@code CatedraClient} y
 * {@code CatalogoClient} de verdad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EstadoDependenciasAdapter")
class EstadoDependenciasAdapterTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection conexion;

    private final MetricasObservabilidad metricas = new MetricasObservabilidad(new SimpleMeterRegistry());

    private EstadoDependenciasAdapter adapter;

    private void construirAdapter() {
        adapter = new EstadoDependenciasAdapter(dataSource, metricas);
    }

    @Test
    @DisplayName("una conexion valida deja la base de datos DISPONIBLE")
    void unaConexionValidaDejaLaBaseDisponible() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(true);
        construirAdapter();

        EstadoDependencias estado = adapter.consultar();

        assertThat(estado.getBaseDeDatos()).isEqualTo(EstadoDependencia.DISPONIBLE);
    }

    @Test
    @DisplayName("una conexion que no pasa isValid deja la base DEGRADADO")
    void unaConexionInvalidaDejaLaBaseDegradada() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(false);
        construirAdapter();

        assertThat(adapter.consultar().getBaseDeDatos()).isEqualTo(EstadoDependencia.DEGRADADO);
    }

    @Test
    @DisplayName("un SQLException al obtener la conexion deja la base DEGRADADO, no lanza")
    void unSqlExceptionDejaLaBaseDegradadaSinLanzar() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("conexion rechazada"));
        construirAdapter();

        assertThat(adapter.consultar().getBaseDeDatos()).isEqualTo(EstadoDependencia.DEGRADADO);
    }

    @Test
    @DisplayName("sin ninguna llamada real todavia, la catedra y el catalogo quedan DESCONOCIDO")
    void sinLlamadasPreviasQuedanDesconocidas() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(true);
        construirAdapter();

        EstadoDependencias estado = adapter.consultar();

        assertThat(estado.getCatedra()).isEqualTo(EstadoDependencia.DESCONOCIDO);
        assertThat(estado.getCatalogo()).isEqualTo(EstadoDependencia.DESCONOCIDO);
    }

    @Test
    @DisplayName("el ultimo resultado conocido de la catedra y el catalogo se traduce sin generar una llamada nueva")
    void elUltimoResultadoConocidoSeTraduceSinLlamadaNueva() throws SQLException {
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.isValid(2)).thenReturn(true);
        construirAdapter();
        metricas.exitoIntegracion(Integracion.CATEDRA);
        metricas.falloIntegracion(Integracion.CATALOGO);

        EstadoDependencias estado = adapter.consultar();

        assertThat(estado.getCatedra()).isEqualTo(EstadoDependencia.DISPONIBLE);
        assertThat(estado.getCatalogo()).isEqualTo(EstadoDependencia.DEGRADADO);
    }

}

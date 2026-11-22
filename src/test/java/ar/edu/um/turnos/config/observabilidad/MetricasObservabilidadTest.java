package ar.edu.um.turnos.config.observabilidad;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cubre los contadores y el ultimo estado conocido que exige el issue #40.
 */
@DisplayName("MetricasObservabilidad")
class MetricasObservabilidadTest {

    private SimpleMeterRegistry registry;
    private MetricasObservabilidad metricas;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricas = new MetricasObservabilidad(registry);
    }

    @Test
    @DisplayName("antes de cualquier llamada, el estado de cada integracion es desconocido")
    void antesDeCualquierLlamadaElEstadoEsDesconocido() {
        assertThat(metricas.estadoDe(Integracion.CATEDRA).esConocido()).isFalse();
        assertThat(metricas.estadoDe(Integracion.CATALOGO).esConocido()).isFalse();
    }

    @Test
    @DisplayName("exitoIntegracion marca la integracion como disponible")
    void exitoIntegracionMarcaDisponible() {
        metricas.exitoIntegracion(Integracion.CATEDRA);

        EstadoIntegracion estado = metricas.estadoDe(Integracion.CATEDRA);
        assertThat(estado.esConocido()).isTrue();
        assertThat(estado.disponible()).isTrue();
    }

    @Test
    @DisplayName("falloIntegracion marca la integracion como no disponible e incrementa el contador de fallos")
    void falloIntegracionMarcaNoDisponibleEIncrementaElContador() {
        metricas.falloIntegracion(Integracion.CATALOGO);

        EstadoIntegracion estado = metricas.estadoDe(Integracion.CATALOGO);
        assertThat(estado.disponible()).isFalse();
        assertThat(registry.get("turnos.integracion.fallos").tag("destino", "catalogo").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("reintentoIntegracion incrementa el contador sin tocar el estado conocido")
    void reintentoIntegracionIncrementaElContadorSinTocarElEstado() {
        metricas.reintentoIntegracion(Integracion.CATEDRA);

        assertThat(registry.get("turnos.integracion.reintentos").tag("destino", "catedra").counter().count())
                .isEqualTo(1.0);
        assertThat(metricas.estadoDe(Integracion.CATEDRA).esConocido())
                .as("un reintento todavia no es un resultado final")
                .isFalse();
    }

    @Test
    @DisplayName("un exito posterior a un fallo deja la integracion disponible otra vez")
    void unExitoPosteriorAUnFalloDejaLaIntegracionDisponibleOtraVez() {
        metricas.falloIntegracion(Integracion.CATEDRA);
        metricas.exitoIntegracion(Integracion.CATEDRA);

        assertThat(metricas.estadoDe(Integracion.CATEDRA).disponible()).isTrue();
    }

    @Test
    @DisplayName("eventoDuplicadoDescartado y procesoVencidoCerrado incrementan sus propios contadores")
    void eventoDuplicadoYProcesoVencidoIncrementanSusContadores() {
        metricas.eventoDuplicadoDescartado();
        metricas.eventoDuplicadoDescartado();
        metricas.procesoVencidoCerrado();

        assertThat(registry.get("turnos.reservas.eventos.duplicados").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("turnos.reservas.procesos.vencidos").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("CATEDRA y CATALOGO llevan contadores y estados independientes")
    void catedraYCatalogoSonIndependientes() {
        metricas.exitoIntegracion(Integracion.CATEDRA);

        assertThat(metricas.estadoDe(Integracion.CATEDRA).disponible()).isTrue();
        assertThat(metricas.estadoDe(Integracion.CATALOGO).esConocido()).isFalse();
    }

}

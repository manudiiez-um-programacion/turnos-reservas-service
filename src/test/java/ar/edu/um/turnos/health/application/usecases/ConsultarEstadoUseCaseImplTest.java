package ar.edu.um.turnos.health.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.health.domain.model.EstadoDependencia;
import ar.edu.um.turnos.health.domain.model.EstadoDependencias;
import ar.edu.um.turnos.health.domain.model.EstadoServicio;
import ar.edu.um.turnos.health.domain.ports.out.EstadoDependenciasPort;
import ar.edu.um.turnos.health.domain.ports.out.InformacionDelServicioPort;

/**
 * Cubre el calculo del estado agregado (#40, §3 de la referencia): la base
 * de datos caida es DOWN sin importar la catedra o el catalogo; la catedra o
 * el catalogo degradados con la base arriba son DEGRADADO; todo disponible o
 * desconocido (todavia sin llamadas reales) es UP.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarEstadoUseCaseImpl")
class ConsultarEstadoUseCaseImplTest {

    @Mock
    private InformacionDelServicioPort informacionDelServicio;

    @Mock
    private EstadoDependenciasPort estadoDependenciasPort;

    private ConsultarEstadoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarEstadoUseCaseImpl(informacionDelServicio, estadoDependenciasPort);
    }

    @Test
    @DisplayName("todo disponible es UP")
    void todoDisponibleEsUp() {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DISPONIBLE)
                .catalogo(EstadoDependencia.DISPONIBLE)
                .build());

        assertThat(useCase.consultar().getEstado()).isEqualTo(EstadoServicio.Estado.UP);
    }

    @Test
    @DisplayName("catedra y catalogo desconocidos (sin llamadas todavia) no degradan el servicio")
    void dependenciasDesconocidasNoDegradan() {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DESCONOCIDO)
                .catalogo(EstadoDependencia.DESCONOCIDO)
                .build());

        assertThat(useCase.consultar().getEstado()).isEqualTo(EstadoServicio.Estado.UP);
    }

    @Test
    @DisplayName("la catedra degradada con la base arriba deja el servicio DEGRADADO, no DOWN")
    void catedraDegradadaDejaElServicioDegradado() {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DEGRADADO)
                .catalogo(EstadoDependencia.DISPONIBLE)
                .build());

        assertThat(useCase.consultar().getEstado()).isEqualTo(EstadoServicio.Estado.DEGRADADO);
    }

    @Test
    @DisplayName("el catalogo degradado con la base arriba tambien deja el servicio DEGRADADO")
    void catalogoDegradadoDejaElServicioDegradado() {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DISPONIBLE)
                .catedra(EstadoDependencia.DISPONIBLE)
                .catalogo(EstadoDependencia.DEGRADADO)
                .build());

        assertThat(useCase.consultar().getEstado()).isEqualTo(EstadoServicio.Estado.DEGRADADO);
    }

    @Test
    @DisplayName("la base de datos caida es DOWN aunque la catedra y el catalogo esten disponibles")
    void baseDeDatosCaidaEsDown() {
        when(estadoDependenciasPort.consultar()).thenReturn(EstadoDependencias.builder()
                .baseDeDatos(EstadoDependencia.DEGRADADO)
                .catedra(EstadoDependencia.DISPONIBLE)
                .catalogo(EstadoDependencia.DISPONIBLE)
                .build());

        assertThat(useCase.consultar().getEstado()).isEqualTo(EstadoServicio.Estado.DOWN);
    }

}

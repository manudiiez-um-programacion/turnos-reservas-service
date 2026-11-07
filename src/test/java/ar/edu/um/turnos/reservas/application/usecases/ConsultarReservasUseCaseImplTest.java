package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.FiltroReservasUsuario;
import ar.edu.um.turnos.reservas.domain.model.PaginaProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarReservasUseCaseImpl")
class ConsultarReservasUseCaseImplTest {

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    private ConsultarReservasUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarReservasUseCaseImpl(procesoReservaRepositoryPort);
    }

    @Test
    @DisplayName("exige un usuarioId")
    void exigeUnUsuarioId() {
        FiltroReservasUsuario filtro = FiltroReservasUsuario.builder().pagina(0).tamanio(20).build();

        assertThatThrownBy(() -> useCase.consultar(filtro)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("delega en el repositorio con el filtro del usuario")
    void delegaEnElRepositorio() {
        FiltroReservasUsuario filtro = FiltroReservasUsuario.builder()
                .usuarioId(7L)
                .estado(EstadoProcesoReserva.CONFIRMADA)
                .pagina(0)
                .tamanio(20)
                .build();
        ProcesoReserva proceso = ProcesoReserva.builder().holdId("hold-1").usuarioId(7L).build();
        PaginaProcesoReserva esperado = new PaginaProcesoReserva(List.of(proceso), 1L);
        when(procesoReservaRepositoryPort.buscarPorUsuario(filtro)).thenReturn(esperado);

        PaginaProcesoReserva resultado = useCase.consultar(filtro);

        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    @DisplayName("sanea una pagina negativa a 0")
    void saneaUnaPaginaNegativa() {
        FiltroReservasUsuario filtro = FiltroReservasUsuario.builder().usuarioId(7L).pagina(-3).tamanio(20).build();
        when(procesoReservaRepositoryPort.buscarPorUsuario(argThat(f -> f.pagina() == 0 && f.tamanio() == 20)))
                .thenReturn(new PaginaProcesoReserva(List.of(), 0L));

        useCase.consultar(filtro);
    }

    @Test
    @DisplayName("sanea un tamanio invalido al valor por defecto")
    void saneaUnTamanioInvalido() {
        FiltroReservasUsuario filtro = FiltroReservasUsuario.builder().usuarioId(7L).pagina(0).tamanio(0).build();
        when(procesoReservaRepositoryPort.buscarPorUsuario(argThat(f -> f.tamanio() == 20)))
                .thenReturn(new PaginaProcesoReserva(List.of(), 0L));

        useCase.consultar(filtro);
    }

    @Test
    @DisplayName("limita un tamanio por encima del maximo permitido")
    void limitaUnTamanioExcesivo() {
        FiltroReservasUsuario filtro = FiltroReservasUsuario.builder().usuarioId(7L).pagina(0).tamanio(1000).build();
        when(procesoReservaRepositoryPort.buscarPorUsuario(argThat(f -> f.tamanio() == 50)))
                .thenReturn(new PaginaProcesoReserva(List.of(), 0L));

        useCase.consultar(filtro);
    }

}

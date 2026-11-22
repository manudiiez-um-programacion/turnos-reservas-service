package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.application.exception.HoldNoEncontradoException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarHoldUseCaseImpl")
class ConsultarHoldUseCaseImplTest {

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    private ConsultarHoldUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarHoldUseCaseImpl(procesoReservaRepositoryPort);
    }

    @Test
    @DisplayName("devuelve el proceso cuando el hold existe y el usuario es su dueño")
    void devuelveElProcesoDelDueño() {
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .usuarioId(42L)
                .estado(EstadoProcesoReserva.ESPERANDO_TELEFONO)
                .build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        ProcesoReserva resultado = useCase.consultar("hold-6c0c2f41", 42L);

        assertThat(resultado).isSameAs(proceso);
    }

    @Test
    @DisplayName("lanza HoldNoEncontradoException si el holdId no corresponde a ningun proceso local")
    void lanzaHoldNoEncontradoSiNoExiste() {
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.consultar("hold-inexistente", 42L))
                .isInstanceOf(HoldNoEncontradoException.class);
    }

    @Test
    @DisplayName("lanza ProcesoReservaAjenoException si el usuario autenticado no es el dueño del proceso")
    void lanzaProcesoReservaAjenoSiElUsuarioNoEsElDueño() {
        ProcesoReserva proceso = ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .usuarioId(42L)
                .estado(EstadoProcesoReserva.ESPERANDO_TELEFONO)
                .build();
        when(procesoReservaRepositoryPort.buscarPorHoldId("hold-6c0c2f41")).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.consultar("hold-6c0c2f41", 99L))
                .isInstanceOf(ProcesoReservaAjenoException.class);
    }

}

package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.reservas.application.exception.TransicionProcesoReservaInvalidaException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.TransicionProcesoReserva;

@DisplayName("TransicionarProcesoReservaUseCaseImpl")
class TransicionarProcesoReservaUseCaseImplTest {

    private final TransicionarProcesoReservaUseCaseImpl useCase = new TransicionarProcesoReservaUseCaseImpl();

    private ProcesoReserva procesoDePrueba(EstadoProcesoReserva estadoActual) {
        return ProcesoReserva.builder()
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .estado(estadoActual)
                .build();
    }

    @Test
    @DisplayName("aplica una transicion valida y la agrega al historial")
    void aplicaUnaTransicionValida() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.HOLD_CREADO);
        Instant ocurridoEn = Instant.parse("2026-09-10T14:36:00Z");

        ProcesoReserva resultado = useCase.transicionar(
                proceso, EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA, ocurridoEn);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA);
        assertThat(resultado.getHistorial())
                .containsExactly(new TransicionProcesoReserva(EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA, ocurridoEn));
    }

    @Test
    @DisplayName("rechaza una transicion invalida sin mutar el proceso")
    void rechazaUnaTransicionInvalida() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.VENCIDA);

        assertThatThrownBy(() -> useCase.transicionar(proceso, EstadoProcesoReserva.CONFIRMADA, Instant.now()))
                .isInstanceOf(TransicionProcesoReservaInvalidaException.class)
                .hasMessageContaining("process-a92b47d0")
                .hasMessageContaining("VENCIDA")
                .hasMessageContaining("CONFIRMADA");

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.VENCIDA);
        assertThat(proceso.getHistorial()).isEmpty();
    }

    @Test
    @DisplayName("rechaza que un estado final retroceda a un estado intermedio")
    void rechazaQueUnEstadoFinalRetroceda() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMADA);

        assertThatThrownBy(() -> useCase.transicionar(proceso, EstadoProcesoReserva.ESPERANDO_TELEFONO, Instant.now()))
                .isInstanceOf(TransicionProcesoReservaInvalidaException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("permite cancelar una reserva confirmada")
    void permiteCancelarUnaReservaConfirmada() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMADA);

        useCase.transicionar(proceso, EstadoProcesoReserva.CANCELADA, Instant.now());

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CANCELADA);
    }

}

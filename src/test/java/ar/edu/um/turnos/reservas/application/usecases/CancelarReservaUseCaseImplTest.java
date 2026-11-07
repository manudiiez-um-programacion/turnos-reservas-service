package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException;
import ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException;
import ar.edu.um.turnos.reservas.domain.model.CancelacionReservaCatedra;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudCancelacionReserva;
import ar.edu.um.turnos.reservas.domain.ports.out.CancelarReservaCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelarReservaUseCaseImpl")
class CancelarReservaUseCaseImplTest {

    @Mock
    private ProcesoReservaRepositoryPort procesoReservaRepositoryPort;

    @Mock
    private CancelarReservaCatedraPort cancelarReservaCatedraPort;

    private final TransicionarProcesoReservaUseCaseImpl transicionarProcesoReservaUseCase =
            new TransicionarProcesoReservaUseCaseImpl();

    private CancelarReservaUseCaseImpl useCase;

    private final SolicitudCancelacionReserva solicitud = SolicitudCancelacionReserva.builder()
            .reservationProcessId("process-a92b47d0")
            .usuarioId(7L)
            .reason("Cambio solicitado por el usuario")
            .build();

    @BeforeEach
    void setUp() {
        useCase = new CancelarReservaUseCaseImpl(
                procesoReservaRepositoryPort, cancelarReservaCatedraPort, transicionarProcesoReservaUseCase);
    }

    private ProcesoReserva procesoDePrueba(EstadoProcesoReserva estado) {
        return ProcesoReserva.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .usuarioId(7L)
                .professionalId(15L)
                .fecha(LocalDate.of(2026, 9, 14))
                .horaInicio(LocalTime.of(9, 30))
                .reservationId(8251L)
                .estado(estado)
                .build();
    }

    @Test
    @DisplayName("camino feliz: cancela en la catedra y transiciona a CANCELADA con cancelledAt")
    void caminoFeliz() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMADA);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(procesoReservaRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        Instant cancelledAt = Instant.parse("2026-09-11T15:10:00Z");
        when(cancelarReservaCatedraPort.cancelar(proceso, "Cambio solicitado por el usuario"))
                .thenReturn(new CancelacionReservaCatedra(8251L, "CANCELLED", cancelledAt));

        ProcesoReserva resultado = useCase.cancelar(solicitud);

        assertThat(resultado.getEstado()).isEqualTo(EstadoProcesoReserva.CANCELADA);
        assertThat(resultado.getCancelledAt()).isEqualTo(cancelledAt);
        assertThat(resultado.getCancelReason()).isEqualTo("Cambio solicitado por el usuario");
        assertThat(resultado.getHistorial())
                .extracting(transicion -> transicion.estado())
                .containsExactly(EstadoProcesoReserva.CANCELADA);
    }

    @Test
    @DisplayName("cancelacion repetida localmente: ya CANCELADA, no vuelve a llamar a la catedra")
    void cancelacionRepetidaLocalmente() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CANCELADA);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        ProcesoReserva resultado = useCase.cancelar(solicitud);

        assertThat(resultado).isSameAs(proceso);
        verify(cancelarReservaCatedraPort, never()).cancelar(any(), any());
        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("un proceso que todavia no esta CONFIRMADA se rechaza localmente, sin llamar a la catedra")
    void procesoNoConfirmadaSeRechazaLocalmente() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.ESPERANDO_TELEFONO);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.cancelar(solicitud)).isInstanceOf(ReservaNoCancelableException.class);

        verify(cancelarReservaCatedraPort, never()).cancelar(any(), any());
    }

    @Test
    @DisplayName("reserva inexistente localmente: no llega a llamar a la catedra")
    void reservaInexistenteLocalmente() {
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.cancelar(solicitud)).isInstanceOf(ReservaNoEncontradaException.class);

        verify(cancelarReservaCatedraPort, never()).cancelar(any(), any());
    }

    @Test
    @DisplayName("solo el dueño del proceso puede cancelarlo")
    void soloElDuenioPuedeCancelar() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMADA);
        proceso.setUsuarioId(99L);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.cancelar(solicitud)).isInstanceOf(ProcesoReservaAjenoException.class);

        verify(cancelarReservaCatedraPort, never()).cancelar(any(), any());
    }

    @Test
    @DisplayName("RESERVATION_NOT_FOUND de la catedra se registra y se relanza sin tocar el estado")
    void reservaNoEncontradaEnLaCatedraSeRelanza() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMADA);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(cancelarReservaCatedraPort.cancelar(proceso, "Cambio solicitado por el usuario"))
                .thenThrow(new ReservaNoEncontradaException("process-a92b47d0", null));

        assertThatThrownBy(() -> useCase.cancelar(solicitud)).isInstanceOf(ReservaNoEncontradaException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMADA);
        verify(procesoReservaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("RESERVATION_OWNER_MISMATCH de la catedra se registra y se relanza sin tocar el estado")
    void reservaOwnerMismatchSeRelanza() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMADA);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(cancelarReservaCatedraPort.cancelar(proceso, "Cambio solicitado por el usuario"))
                .thenThrow(new ReservaOwnerMismatchException("process-a92b47d0", null));

        assertThatThrownBy(() -> useCase.cancelar(solicitud)).isInstanceOf(ReservaOwnerMismatchException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("RESERVATION_NOT_CANCELLABLE de la catedra se registra y se relanza sin tocar el estado")
    void reservaNoCancelableEnLaCatedraSeRelanza() {
        ProcesoReserva proceso = procesoDePrueba(EstadoProcesoReserva.CONFIRMADA);
        when(procesoReservaRepositoryPort.buscarPorReservationProcessId("process-a92b47d0"))
                .thenReturn(Optional.of(proceso));
        when(cancelarReservaCatedraPort.cancelar(proceso, "Cambio solicitado por el usuario"))
                .thenThrow(new ReservaNoCancelableException("process-a92b47d0", null));

        assertThatThrownBy(() -> useCase.cancelar(solicitud)).isInstanceOf(ReservaNoCancelableException.class);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProcesoReserva.CONFIRMADA);
    }

}

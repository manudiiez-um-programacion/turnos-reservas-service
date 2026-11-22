package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.CancelacionReservaResponse;
import ar.edu.um.turnos.config.catedra.exception.CatedraClientErrorException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException;
import ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException;
import ar.edu.um.turnos.reservas.domain.model.CancelacionReservaCatedra;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;

@ExtendWith(MockitoExtension.class)
class CancelarReservaCatedraAdapterTest {

    private final ProcesoReserva proceso = ProcesoReserva.builder()
            .holdId("hold-6c0c2f41")
            .reservationProcessId("process-a92b47d0")
            .professionalId(15L)
            .fecha(LocalDate.of(2026, 9, 14))
            .horaInicio(LocalTime.of(9, 30))
            .build();

    @Mock
    private CatedraClient catedraClient;

    private CancelarReservaCatedraAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CancelarReservaCatedraAdapter(catedraClient);
    }

    @Test
    void delegaEnCatedraClientYTraduceLaRespuesta() {
        Instant cancelledAt = Instant.parse("2026-09-11T15:10:00Z");
        when(catedraClient.cancelarReserva("process-a92b47d0", "motivo"))
                .thenReturn(new CancelacionReservaResponse(8251L, "process-a92b47d0", "CANCELLED", cancelledAt));

        CancelacionReservaCatedra resultado = adapter.cancelar(proceso, "motivo");

        assertThat(resultado).isEqualTo(new CancelacionReservaCatedra(8251L, "CANCELLED", cancelledAt));
    }

    @Test
    void traduceRESERVATION_NOT_FOUND() {
        when(catedraClient.cancelarReserva("process-a92b47d0", "motivo"))
                .thenThrow(new CatedraClientErrorException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.cancelar(proceso, "motivo"))
                .isInstanceOf(ReservaNoEncontradaException.class);
    }

    @Test
    void traduceRESERVATION_OWNER_MISMATCH() {
        when(catedraClient.cancelarReserva("process-a92b47d0", "motivo"))
                .thenThrow(new CatedraClientErrorException(
                        HttpStatus.FORBIDDEN, "RESERVATION_OWNER_MISMATCH", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.cancelar(proceso, "motivo"))
                .isInstanceOf(ReservaOwnerMismatchException.class);
    }

    @Test
    void traduceRESERVATION_NOT_CANCELLABLE() {
        when(catedraClient.cancelarReserva("process-a92b47d0", "motivo"))
                .thenThrow(new CatedraClientErrorException(
                        HttpStatus.CONFLICT, "RESERVATION_NOT_CANCELLABLE", "detalle", List.of()));

        assertThatThrownBy(() -> adapter.cancelar(proceso, "motivo"))
                .isInstanceOf(ReservaNoCancelableException.class);
    }

    @Test
    void noTraduceOtrosErroresFuncionales() {
        CatedraClientErrorException original =
                new CatedraClientErrorException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "detalle", List.of());
        when(catedraClient.cancelarReserva("process-a92b47d0", "motivo")).thenThrow(original);

        assertThatThrownBy(() -> adapter.cancelar(proceso, "motivo")).isSameAs(original);
    }

}

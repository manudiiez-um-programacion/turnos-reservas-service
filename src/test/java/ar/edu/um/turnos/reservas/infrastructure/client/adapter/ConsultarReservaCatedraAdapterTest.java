package ar.edu.um.turnos.reservas.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.config.catedra.CatedraClient;
import ar.edu.um.turnos.config.catedra.dto.ListarReservasFiltro;
import ar.edu.um.turnos.config.catedra.dto.ListarReservasResponse;
import ar.edu.um.turnos.config.catedra.dto.ReservaResponse;
import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;

@ExtendWith(MockitoExtension.class)
class ConsultarReservaCatedraAdapterTest {

    @Mock
    private CatedraClient catedraClient;

    private ConsultarReservaCatedraAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ConsultarReservaCatedraAdapter(catedraClient);
    }

    private ReservaResponse reservaDePrueba(String reservationProcessId, String status, Instant cancelledAt, String cancellationReason) {
        return new ReservaResponse(
                8251L, reservationProcessId, "user-42", null, null, null, null, null, null, null, null, null,
                status, null, null, cancelledAt, cancellationReason);
    }

    @Test
    void filtraPorExternalPatientIdYEncuentraElReservationProcessIdBuscado() {
        Instant cancelledAt = Instant.parse("2026-09-11T15:10:00Z");
        ReservaResponse otraReserva = reservaDePrueba("process-otro", "CONFIRMED", null, null);
        ReservaResponse buscada = reservaDePrueba("process-a92b47d0", "CANCELLED", cancelledAt, "Cambio solicitado por el usuario");
        when(catedraClient.listarReservas(any(ListarReservasFiltro.class)))
                .thenReturn(new ListarReservasResponse(List.of(otraReserva, buscada), 2));

        Optional<ReservaCatedraEncontrada> resultado = adapter.buscarPorReservationProcessId("user-42", "process-a92b47d0");

        assertThat(resultado).contains(new ReservaCatedraEncontrada(
                8251L, "CANCELLED", cancelledAt, "Cambio solicitado por el usuario"));

        ArgumentCaptor<ListarReservasFiltro> captor = ArgumentCaptor.forClass(ListarReservasFiltro.class);
        verify(catedraClient).listarReservas(captor.capture());
        assertThat(captor.getValue().externalPatientId()).isEqualTo("user-42");
    }

    @Test
    void siNoEncuentraElReservationProcessIdDevuelveVacio() {
        when(catedraClient.listarReservas(any(ListarReservasFiltro.class)))
                .thenReturn(new ListarReservasResponse(List.of(reservaDePrueba("process-otro", "CONFIRMED", null, null)), 1));

        Optional<ReservaCatedraEncontrada> resultado = adapter.buscarPorReservationProcessId("user-42", "process-a92b47d0");

        assertThat(resultado).isEmpty();
    }

    @Test
    void unaListaVaciaEsUnaRespuestaValida() {
        when(catedraClient.listarReservas(any(ListarReservasFiltro.class)))
                .thenReturn(new ListarReservasResponse(List.of(), 0));

        Optional<ReservaCatedraEncontrada> resultado = adapter.buscarPorReservationProcessId("user-42", "process-a92b47d0");

        assertThat(resultado).isEmpty();
    }

}

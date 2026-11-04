package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.domain.ports.out.RegistrarEventoProcesadoPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcesarEventoAccionUseCaseImpl")
class ProcesarEventoAccionUseCaseImplTest {

    @Mock
    private RegistrarEventoProcesadoPort registrarEventoProcesadoPort;

    private ProcesarEventoAccionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcesarEventoAccionUseCaseImpl(registrarEventoProcesadoPort);
    }

    private EventoAccionReserva.EventoAccionReservaBuilder eventoValido() {
        return EventoAccionReserva.builder()
                .eventId("confirmation-event-7d10")
                .tipo(TipoEventoAccion.RESERVA_CONFIRMADA)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .schemaVersion(1);
    }

    @Test
    @DisplayName("registra el evento como procesado y no lanza para un tipo conocido con schemaVersion 1")
    void registraYNoLanzaParaUnTipoConocidoConSchemaVersionSoportada() {
        EventoAccionReserva evento = eventoValido().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(true);

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort).intentarRegistrar(evento);
    }

    @Test
    @DisplayName("descarta sin lanzar un eventId ya procesado, sin repetir su efecto")
    void descartaSinLanzarUnEventoYaProcesado() {
        EventoAccionReserva evento = eventoValido().build();
        when(registrarEventoProcesadoPort.intentarRegistrar(evento)).thenReturn(false);

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort).intentarRegistrar(evento);
    }

    @Test
    @DisplayName("descarta sin lanzar un eventType desconocido, sin consultar la deduplicacion")
    void descartaSinLanzarUnEventTypeDesconocido() {
        EventoAccionReserva evento = eventoValido().tipo(TipoEventoAccion.DESCONOCIDO).build();

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort, never()).intentarRegistrar(evento);
    }

    @Test
    @DisplayName("descarta sin lanzar una schemaVersion no soportada, sin consultar la deduplicacion")
    void descartaSinLanzarUnaSchemaVersionNoSoportada() {
        EventoAccionReserva evento = eventoValido().schemaVersion(2).build();

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();

        verify(registrarEventoProcesadoPort, never()).intentarRegistrar(evento);
    }

}

package ar.edu.um.turnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;

@DisplayName("ProcesarEventoAccionUseCaseImpl")
class ProcesarEventoAccionUseCaseImplTest {

    private final ProcesarEventoAccionUseCaseImpl useCase = new ProcesarEventoAccionUseCaseImpl();

    private EventoAccionReserva.EventoAccionReservaBuilder eventoValido() {
        return EventoAccionReserva.builder()
                .eventId("confirmation-event-7d10")
                .tipo(TipoEventoAccion.RESERVA_CONFIRMADA)
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .schemaVersion(1);
    }

    @Test
    @DisplayName("no lanza para un tipo conocido con schemaVersion 1")
    void noLanzaParaUnTipoConocidoConSchemaVersionSoportada() {
        assertThatCode(() -> useCase.procesar(eventoValido().build())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("descarta sin lanzar un eventType desconocido")
    void descartaSinLanzarUnEventTypeDesconocido() {
        EventoAccionReserva evento = eventoValido().tipo(TipoEventoAccion.DESCONOCIDO).build();

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("descarta sin lanzar una schemaVersion no soportada")
    void descartaSinLanzarUnaSchemaVersionNoSoportada() {
        EventoAccionReserva evento = eventoValido().schemaVersion(2).build();

        assertThatCode(() -> useCase.procesar(evento)).doesNotThrowAnyException();
    }

}

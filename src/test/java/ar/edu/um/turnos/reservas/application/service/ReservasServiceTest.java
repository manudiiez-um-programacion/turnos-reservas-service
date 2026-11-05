package ar.edu.um.turnos.reservas.application.service;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;

@ExtendWith(MockitoExtension.class)
class ReservasServiceTest {

    @Mock
    private ProcesarEventoAccionUseCase procesarEventoAccionUseCase;

    @Mock
    private TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;

    @Test
    void delegaEnElCasoDeUsoDeProcesarEvento() {
        ReservasService service = new ReservasService(procesarEventoAccionUseCase, transicionarProcesoReservaUseCase);
        EventoAccionReserva evento = EventoAccionReserva.builder().eventId("un-evento").build();

        service.procesarEventoAccion(evento);

        verify(procesarEventoAccionUseCase).procesar(evento);
    }

    @Test
    void delegaEnElCasoDeUsoDeTransicionar() {
        ReservasService service = new ReservasService(procesarEventoAccionUseCase, transicionarProcesoReservaUseCase);
        ProcesoReserva proceso = ProcesoReserva.builder().reservationProcessId("process-1").build();
        Instant ocurridoEn = Instant.parse("2026-09-10T14:36:00Z");

        service.transicionar(proceso, EstadoProcesoReserva.CONFIRMADA, ocurridoEn);

        verify(transicionarProcesoReservaUseCase).transicionar(proceso, EstadoProcesoReserva.CONFIRMADA, ocurridoEn);
    }

}

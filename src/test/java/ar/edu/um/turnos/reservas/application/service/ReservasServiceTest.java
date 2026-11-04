package ar.edu.um.turnos.reservas.application.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;

@ExtendWith(MockitoExtension.class)
class ReservasServiceTest {

    @Mock
    private ProcesarEventoAccionUseCase procesarEventoAccionUseCase;

    @Test
    void delegaEnElCasoDeUso() {
        ReservasService service = new ReservasService(procesarEventoAccionUseCase);
        EventoAccionReserva evento = EventoAccionReserva.builder().eventId("un-evento").build();

        service.procesarEventoAccion(evento);

        verify(procesarEventoAccionUseCase).procesar(evento);
    }

}

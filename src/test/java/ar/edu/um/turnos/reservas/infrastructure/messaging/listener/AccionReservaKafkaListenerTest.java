package ar.edu.um.turnos.reservas.infrastructure.messaging.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.turnos.reservas.application.service.ReservasService;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.infrastructure.messaging.mapper.EventoAccionMessagingMapper;

@ExtendWith(MockitoExtension.class)
class AccionReservaKafkaListenerTest {

    @Mock
    private EventoAccionMessagingMapper eventoAccionMessagingMapper;

    @Mock
    private ReservasService reservasService;

    private AccionReservaKafkaListener listener;

    @Test
    void mapeaYDespachaElRegistroRecibido() {
        listener = new AccionReservaKafkaListener(eventoAccionMessagingMapper, reservasService);

        ConsumerRecord<String, String> registro =
                new ConsumerRecord<>("alumnos.turnos.acciones.proyecto-de-prueba", 0, 0, "process-a92b47d0", "{}");
        EventoAccionReserva evento = EventoAccionReserva.builder().eventId("confirmation-event-7d10").build();
        when(eventoAccionMessagingMapper.aDominio(eq("process-a92b47d0"), eq("{}"))).thenReturn(evento);

        listener.escuchar(registro);

        verify(reservasService).procesarEventoAccion(evento);
    }

}

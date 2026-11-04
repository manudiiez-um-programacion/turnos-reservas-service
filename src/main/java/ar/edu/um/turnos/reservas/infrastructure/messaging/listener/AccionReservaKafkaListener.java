package ar.edu.um.turnos.reservas.infrastructure.messaging.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.application.service.ReservasService;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.infrastructure.messaging.mapper.EventoAccionMessagingMapper;
import lombok.RequiredArgsConstructor;

/**
 * Oyente de {@code alumnos.turnos.acciones.{groupId}} (#18, §15.2 y §15.4-§15.10
 * de la referencia).
 *
 * <p>Solo deserializa, mapea y delega en {@link ReservasService}: la
 * infraestructura no decide si un evento se procesa o se descarta, eso es
 * responsabilidad del caso de uso. El {@code autoStartup} es configurable
 * para poder apagar el consumidor en el perfil de pruebas, donde
 * {@code catedra.kafka.bootstrap-servers} es un host inventado y no hay
 * broker con el que conectarse.
 */
@Component
@RequiredArgsConstructor
public class AccionReservaKafkaListener {

    private final EventoAccionMessagingMapper eventoAccionMessagingMapper;
    private final ReservasService reservasService;

    @KafkaListener(
            topics = "${catedra.kafka.appointment-actions-topic}",
            containerFactory = "accionesReservaKafkaListenerContainerFactory",
            autoStartup = "${turnos.kafka.listener.auto-startup:true}")
    public void escuchar(ConsumerRecord<String, String> registro) {
        EventoAccionReserva evento = eventoAccionMessagingMapper.aDominio(registro.key(), registro.value());
        reservasService.procesarEventoAccion(evento);
    }

}

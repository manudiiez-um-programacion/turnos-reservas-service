package ar.edu.um.turnos.reservas.application.usecases;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso: decide si un evento se descarta (version de
 * esquema no soportada o {@code eventType} desconocido) o se despacha.
 *
 * <p>La logica de cada tipo de evento conocido (solicitud de informacion
 * adicional, resultado final, cancelacion) todavia no existe: la agregan los
 * casos de uso de #29, #31 y #35 sobre el mismo {@link TipoEventoAccion}. Este
 * caso de uso solo garantiza la recepcion observable y el despacho por tipo;
 * el oyente de Kafka confirma el offset recien cuando este metodo retorna sin
 * lanzar, que es la parte de #18 que si hace falta ya.
 */
@Slf4j
@Component
public class ProcesarEventoAccionUseCaseImpl implements ProcesarEventoAccionUseCase {

    private static final int SCHEMA_VERSION_SOPORTADA = 1;

    @Override
    public void procesar(EventoAccionReserva evento) {
        if (evento.getSchemaVersion() != SCHEMA_VERSION_SOPORTADA) {
            log.warn("Evento {} con schemaVersion {} no soportada (se esperaba {}): se descarta "
                            + "(eventId={}, reservationProcessId={})",
                    evento.getTipo(), evento.getSchemaVersion(), SCHEMA_VERSION_SOPORTADA,
                    evento.getEventId(), evento.getReservationProcessId());
            return;
        }
        if (evento.getTipo() == TipoEventoAccion.DESCONOCIDO) {
            log.warn("Evento con eventType desconocido: se descarta (eventId={}, reservationProcessId={})",
                    evento.getEventId(), evento.getReservationProcessId());
            return;
        }

        log.info("Evento {} recibido para el proceso de reserva {} (eventId={})",
                evento.getTipo(), evento.getReservationProcessId(), evento.getEventId());
    }

}

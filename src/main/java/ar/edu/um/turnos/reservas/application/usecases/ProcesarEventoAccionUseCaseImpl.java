package ar.edu.um.turnos.reservas.application.usecases;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.RegistrarEventoProcesadoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso: decide si un evento se descarta (version de
 * esquema no soportada o {@code eventType} desconocido), se descarta porque
 * ya se proceso ese {@code eventId} (#20, §18.1 de la referencia), o se
 * despacha.
 *
 * <p>La logica de cada tipo de evento conocido (solicitud de informacion
 * adicional, resultado final, cancelacion) todavia no existe: la agregan los
 * casos de uso de #29, #31 y #35 sobre el mismo {@link TipoEventoAccion},
 * dentro de este mismo metodo y por lo tanto de la misma transaccion que
 * {@link RegistrarEventoProcesadoPort#intentarRegistrar}: si el efecto falla,
 * la marca de "procesado" tiene que revertirse con el, para que un reintento
 * no trate el evento como un duplicado que nunca llego a tener efecto.
 *
 * <p>Los eventos que se descartan por schemaVersion o eventType no se
 * registran en la tabla de deduplicacion: descartarlos es siempre el mismo
 * resultado, sin importar cuantas veces llegue el mismo eventId, asi que no
 * hace falta marcarlos.
 *
 * <p>El oyente de Kafka confirma el offset recien cuando este metodo retorna
 * sin lanzar, que es la parte de #18 que si hace falta ya.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcesarEventoAccionUseCaseImpl implements ProcesarEventoAccionUseCase {

    private static final int SCHEMA_VERSION_SOPORTADA = 1;

    private final RegistrarEventoProcesadoPort registrarEventoProcesadoPort;

    @Override
    @Transactional
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

        if (!registrarEventoProcesadoPort.intentarRegistrar(evento)) {
            log.info("Evento {} con eventId {} ya fue procesado: se descarta sin repetir su efecto "
                            + "(reservationProcessId={})",
                    evento.getTipo(), evento.getEventId(), evento.getReservationProcessId());
            return;
        }

        log.info("Evento {} recibido para el proceso de reserva {} (eventId={})",
                evento.getTipo(), evento.getReservationProcessId(), evento.getEventId());
    }

}

package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.EventoAccionReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.TipoEventoAccion;
import ar.edu.um.turnos.reservas.domain.ports.in.ProcesarEventoAccionUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import ar.edu.um.turnos.reservas.domain.ports.out.RegistrarEventoProcesadoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso: decide si un evento se descarta (version de
 * esquema no soportada o {@code eventType} desconocido), se descarta porque
 * ya se proceso ese {@code eventId} (#20, §18.1 de la referencia), o se
 * despacha.
 *
 * <p>La logica de cada tipo de evento conocido se agrega sobre el mismo
 * {@link TipoEventoAccion}, dentro de este mismo metodo y por lo tanto de la
 * misma transaccion que {@link RegistrarEventoProcesadoPort#intentarRegistrar}:
 * si el efecto falla, la marca de "procesado" tiene que revertirse con el,
 * para que un reintento no trate el evento como un duplicado que nunca llego
 * a tener efecto. {@link TipoEventoAccion#SOLICITUD_INFORMACION_ADICIONAL} ya
 * esta implementado (#29); el resultado final y la cancelacion todavia no
 * (#31, #35).
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
    private static final String CAMPO_REQUERIDO_CONOCIDO = "PHONE_NUMBER";

    private final RegistrarEventoProcesadoPort registrarEventoProcesadoPort;
    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;

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

        if (evento.getTipo() == TipoEventoAccion.SOLICITUD_INFORMACION_ADICIONAL) {
            procesarSolicitudInformacionAdicional(evento);
        }
    }

    /**
     * {@code AdditionalInformationRequested} (#29, §15.4 de la referencia):
     * guarda {@code requestEventId}/{@code requiredField}/{@code message} y
     * el nuevo {@code expiresAt}, y avanza el proceso a
     * {@link EstadoProcesoReserva#ESPERANDO_TELEFONO} si todavia no esta ahi.
     *
     * <p>Un {@code reservationProcessId} desconocido no es un error: puede
     * ser un proceso perdido por un reinicio, y se reconcilia en #36/#37. Un
     * proceso ya en estado final descarta el pedido sin retroceder. En
     * cualquier otro caso se actualizan los datos del pedido mas reciente,
     * tanto si hace falta transicionar (la confirmacion REST se perdio y este
     * evento es la unica senal de que hay que esperar el telefono) como si
     * el proceso ya estaba esperando el telefono (caso normal: #28 ya lo
     * habia transicionado al recibir el {@code 202}).
     */
    private void procesarSolicitudInformacionAdicional(EventoAccionReserva evento) {
        Optional<ProcesoReserva> procesoEncontrado =
                procesoReservaRepositoryPort.buscarPorReservationProcessId(evento.getReservationProcessId());
        if (procesoEncontrado.isEmpty()) {
            log.warn("AdditionalInformationRequested para un reservationProcessId desconocido {}: puede ser un "
                            + "proceso perdido por un reinicio (eventId={})",
                    evento.getReservationProcessId(), evento.getEventId());
            return;
        }

        ProcesoReserva proceso = procesoEncontrado.get();
        if (proceso.getEstado().esFinal()) {
            log.info("AdditionalInformationRequested descartado: el proceso {} ya esta en estado final {} "
                            + "(eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
            return;
        }

        if (evento.getRequiredField() != null && !CAMPO_REQUERIDO_CONOCIDO.equals(evento.getRequiredField())) {
            log.warn("requiredField '{}' distinto de {} para el proceso {}: se registra igual (eventId={})",
                    evento.getRequiredField(), CAMPO_REQUERIDO_CONOCIDO, proceso.getReservationProcessId(),
                    evento.getEventId());
        }

        proceso.setRequestEventId(evento.getEventId());
        proceso.setRequestRequiredField(evento.getRequiredField());
        proceso.setRequestMessage(evento.getMessage());
        proceso.setExpiresAt(evento.getExpiresAt());

        if (proceso.getEstado().puedeTransicionarA(EstadoProcesoReserva.ESPERANDO_TELEFONO)) {
            proceso = transicionarProcesoReservaUseCase.transicionar(
                    proceso, EstadoProcesoReserva.ESPERANDO_TELEFONO, Instant.now());
        } else {
            log.info("Proceso {} no transiciona desde {}: se actualiza igual el pedido de informacion mas "
                            + "reciente (eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
        }

        procesoReservaRepositoryPort.guardar(proceso);
    }

}

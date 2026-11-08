package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.turnos.config.observabilidad.MetricasObservabilidad;
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
 * a tener efecto. {@link TipoEventoAccion#SOLICITUD_INFORMACION_ADICIONAL}
 * (#29), los cuatro resultados finales ({@link TipoEventoAccion#RESERVA_CONFIRMADA},
 * {@link TipoEventoAccion#INFORMACION_ADICIONAL_RECHAZADA},
 * {@link TipoEventoAccion#PROCESO_VENCIDO}, {@link TipoEventoAccion#PROCESO_INVALIDO})
 * (#31) y la cancelacion ({@link TipoEventoAccion#RESERVA_CANCELADA}, #35) ya
 * estan implementados.
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
    private final MetricasObservabilidad metricas;

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
            metricas.eventoDuplicadoDescartado();
            log.info("Evento {} con eventId {} ya fue procesado: se descarta sin repetir su efecto "
                            + "(reservationProcessId={})",
                    evento.getTipo(), evento.getEventId(), evento.getReservationProcessId());
            return;
        }

        log.info("Evento {} recibido para el proceso de reserva {} (eventId={})",
                evento.getTipo(), evento.getReservationProcessId(), evento.getEventId());

        switch (evento.getTipo()) {
            case SOLICITUD_INFORMACION_ADICIONAL -> procesarSolicitudInformacionAdicional(evento);
            case RESERVA_CONFIRMADA -> procesarReservaConfirmada(evento);
            case INFORMACION_ADICIONAL_RECHAZADA -> procesarInformacionAdicionalRechazada(evento);
            case PROCESO_VENCIDO -> procesarProcesoVencido(evento);
            case PROCESO_INVALIDO -> procesarProcesoInvalido(evento);
            case RESERVA_CANCELADA -> procesarReservaCancelada(evento);
            default -> log.debug("Evento {} sin logica propia todavia (eventId={})",
                    evento.getTipo(), evento.getEventId());
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

    /**
     * {@code AppointmentConfirmed} (#31, §15.6 de la referencia): guarda
     * {@code reservationId} y cierra el proceso como
     * {@link EstadoProcesoReserva#CONFIRMADA}.
     *
     * <p>Transiciona desde el estado que sea (ver el javadoc de
     * {@link EstadoProcesoReserva}): el orden entre REST y Kafka no esta
     * garantizado, y este evento puede llegar antes de que el flujo REST
     * local haya alcanzado {@code TELEFONO_ENVIADO}. Un proceso desconocido
     * o ya en estado final se descarta igual que en
     * {@link #procesarSolicitudInformacionAdicional}.
     */
    private void procesarReservaConfirmada(EventoAccionReserva evento) {
        Optional<ProcesoReserva> procesoEncontrado =
                procesoReservaRepositoryPort.buscarPorReservationProcessId(evento.getReservationProcessId());
        if (procesoEncontrado.isEmpty()) {
            log.warn("AppointmentConfirmed para un reservationProcessId desconocido {}: puede ser un proceso "
                            + "perdido por un reinicio (eventId={})",
                    evento.getReservationProcessId(), evento.getEventId());
            return;
        }

        ProcesoReserva proceso = procesoEncontrado.get();
        if (proceso.getEstado().esFinal()) {
            log.info("AppointmentConfirmed descartado: el proceso {} ya esta en estado final {} (eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
            return;
        }

        proceso.setReservationId(evento.getReservationId());
        proceso = transicionarProcesoReservaUseCase.transicionar(proceso, EstadoProcesoReserva.CONFIRMADA, Instant.now());
        procesoReservaRepositoryPort.guardar(proceso);
    }

    /**
     * {@code AdditionalInformationRejected} (#31, §15.7 de la referencia):
     * guarda {@code reason}/{@code message} y el nuevo {@code expiresAt}, y
     * vuelve a {@link EstadoProcesoReserva#TELEFONO_RECHAZADO} para habilitar
     * un reintento (limitado por {@code EnviarTelefonoUseCaseImpl}).
     *
     * <p>Los datos del rechazo se actualizan aunque no haga falta transicionar
     * (por ejemplo, si el envio local ya lo habia hecho): el motivo mas
     * reciente es el que el usuario tiene que ver al consultar el proceso.
     */
    private void procesarInformacionAdicionalRechazada(EventoAccionReserva evento) {
        Optional<ProcesoReserva> procesoEncontrado =
                procesoReservaRepositoryPort.buscarPorReservationProcessId(evento.getReservationProcessId());
        if (procesoEncontrado.isEmpty()) {
            log.warn("AdditionalInformationRejected para un reservationProcessId desconocido {}: puede ser un "
                            + "proceso perdido por un reinicio (eventId={})",
                    evento.getReservationProcessId(), evento.getEventId());
            return;
        }

        ProcesoReserva proceso = procesoEncontrado.get();
        if (proceso.getEstado().esFinal()) {
            log.info("AdditionalInformationRejected descartado: el proceso {} ya esta en estado final {} "
                            + "(eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
            return;
        }

        proceso.setReason(evento.getReason());
        proceso.setReasonMessage(evento.getMessage());
        proceso.setExpiresAt(evento.getExpiresAt());

        if (proceso.getEstado().puedeTransicionarA(EstadoProcesoReserva.TELEFONO_RECHAZADO)) {
            proceso = transicionarProcesoReservaUseCase.transicionar(
                    proceso, EstadoProcesoReserva.TELEFONO_RECHAZADO, Instant.now());
        } else {
            log.info("Proceso {} no transiciona desde {}: se actualiza igual el motivo del rechazo (eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
        }

        procesoReservaRepositoryPort.guardar(proceso);
    }

    /**
     * {@code AppointmentProcessExpired} (#31, §15.8 y §18.3 de la referencia):
     * guarda {@code reason} y cierra el proceso como
     * {@link EstadoProcesoReserva#VENCIDA}, cortando nuevos intentos de
     * confirmacion. Un evento tardio sobre un proceso ya final no lo reabre.
     */
    private void procesarProcesoVencido(EventoAccionReserva evento) {
        Optional<ProcesoReserva> procesoEncontrado =
                procesoReservaRepositoryPort.buscarPorReservationProcessId(evento.getReservationProcessId());
        if (procesoEncontrado.isEmpty()) {
            log.warn("AppointmentProcessExpired para un reservationProcessId desconocido {}: puede ser un "
                            + "proceso perdido por un reinicio (eventId={})",
                    evento.getReservationProcessId(), evento.getEventId());
            return;
        }

        ProcesoReserva proceso = procesoEncontrado.get();
        if (proceso.getEstado().esFinal()) {
            log.info("AppointmentProcessExpired descartado: el proceso {} ya esta en estado final {} (eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
            return;
        }

        proceso.setReason(evento.getReason());
        proceso = transicionarProcesoReservaUseCase.transicionar(proceso, EstadoProcesoReserva.VENCIDA, Instant.now());
        procesoReservaRepositoryPort.guardar(proceso);
    }

    /**
     * {@code AppointmentProcessInvalid} (#31, §15.9 de la referencia): guarda
     * {@code reason} y marca el proceso como {@link EstadoProcesoReserva#INVALIDA}.
     *
     * <p>La referencia sugiere reconciliar por REST si hace falta confirmar
     * el estado persistido; esa reconciliacion activa es la de #36 y todavia
     * no existe, asi que por ahora esto solo deja registrado el motivo que
     * informo la catedra.
     */
    private void procesarProcesoInvalido(EventoAccionReserva evento) {
        Optional<ProcesoReserva> procesoEncontrado =
                procesoReservaRepositoryPort.buscarPorReservationProcessId(evento.getReservationProcessId());
        if (procesoEncontrado.isEmpty()) {
            log.warn("AppointmentProcessInvalid para un reservationProcessId desconocido {}: puede ser un "
                            + "proceso perdido por un reinicio (eventId={})",
                    evento.getReservationProcessId(), evento.getEventId());
            return;
        }

        ProcesoReserva proceso = procesoEncontrado.get();
        if (proceso.getEstado().esFinal()) {
            log.info("AppointmentProcessInvalid descartado: el proceso {} ya esta en estado final {} (eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
            return;
        }

        proceso.setReason(evento.getReason());
        proceso = transicionarProcesoReservaUseCase.transicionar(proceso, EstadoProcesoReserva.INVALIDA, Instant.now());
        procesoReservaRepositoryPort.guardar(proceso);
    }

    /**
     * {@code AppointmentCancelled} (#35, §15.10 de la referencia): guarda
     * {@code cancelReason} (puede ser {@code null}) y {@code cancelledAt}, y
     * cierra el proceso como {@link EstadoProcesoReserva#CANCELADA}.
     *
     * <p>Solo es alcanzable desde {@link EstadoProcesoReserva#CONFIRMADA}
     * (ver {@link EstadoProcesoReserva#puedeTransicionarA}): la catedra nunca
     * cancela una reserva que no haya confirmado antes, y Kafka preserva el
     * orden de los eventos con la misma key ({@code reservationProcessId}),
     * asi que {@code AppointmentConfirmed} ya tiene que haber llegado. Por
     * eso el chequeo de abajo usa {@code puedeTransicionarA} y no {@code
     * esFinal}: a diferencia de los demas eventos de resultado, el origen
     * valido de esta transicion ({@code CONFIRMADA}) es en si mismo un
     * estado final. Ese chequeo cubre los dos ordenes posibles entre REST
     * (#34) y Kafka: si la cancelacion ya se aplico por REST, el proceso ya
     * esta {@code CANCELADA} y este evento se descarta sin pisar el {@code
     * cancelledAt} ni el {@code cancelReason} originales; si la catedra
     * cancelo primero, este evento es la unica senal y aplica la transicion
     * con los datos que trae.
     */
    private void procesarReservaCancelada(EventoAccionReserva evento) {
        Optional<ProcesoReserva> procesoEncontrado =
                procesoReservaRepositoryPort.buscarPorReservationProcessId(evento.getReservationProcessId());
        if (procesoEncontrado.isEmpty()) {
            log.warn("AppointmentCancelled para un reservationProcessId desconocido {}: puede ser un "
                            + "proceso perdido por un reinicio (eventId={})",
                    evento.getReservationProcessId(), evento.getEventId());
            return;
        }

        ProcesoReserva proceso = procesoEncontrado.get();
        if (!proceso.getEstado().puedeTransicionarA(EstadoProcesoReserva.CANCELADA)) {
            log.info("AppointmentCancelled descartado: el proceso {} esta en estado {} y no admite la "
                            + "cancelacion (eventId={})",
                    proceso.getReservationProcessId(), proceso.getEstado(), evento.getEventId());
            return;
        }

        proceso.setCancelReason(evento.getReason());
        proceso.setCancelledAt(evento.getOccurredAt());
        proceso = transicionarProcesoReservaUseCase.transicionar(proceso, EstadoProcesoReserva.CANCELADA, Instant.now());
        procesoReservaRepositoryPort.guardar(proceso);
    }

}

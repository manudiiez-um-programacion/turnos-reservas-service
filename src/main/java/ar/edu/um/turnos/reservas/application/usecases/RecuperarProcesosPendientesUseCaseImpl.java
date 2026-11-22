package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;
import ar.edu.um.turnos.reservas.domain.ports.in.CerrarProcesosVencidosUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.RecuperarProcesosPendientesUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.ConsultarReservaCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso (#37, §4.2 y §8 del enunciado): revisa cada
 * proceso no final y decide, para cada uno, cerrarlo como vencido,
 * reconciliarlo contra la catedra o dejarlo esperando sin tocarlo.
 *
 * <ul>
 *   <li>{@code expiresAt} ya pasado: se cierra como {@code VENCIDA}
 *       delegando en {@link CerrarProcesosVencidosUseCase} (#38) — el mismo
 *       criterio, con el mismo margen de reloj, que la tarea programada
 *       aplica de forma periodica, aca aplicado una vez al arrancar antes de
 *       decidir que hacer con el resto.</li>
 *   <li>{@link EstadoProcesoReserva#CONFIRMACION_INICIAL_ENVIADA}: es el
 *       unico estado no final en el que ya se intento (o se estaba
 *       intentando) una llamada con efectos antes del reinicio, asi que se
 *       reconcilia igual que #36 — se busca el proceso en
 *       {@code GET /api/appointments}. Si la catedra ya lo tiene, se alinea
 *       a {@link EstadoProcesoReserva#ESPERANDO_TELEFONO}. Si no, o si la
 *       catedra no responde, se deja el proceso como esta: no se reintenta
 *       el {@code confirm} desde aca, para que la recuperacion nunca falle
 *       ni se cuelgue por una catedra caida. Un proximo intento del usuario,
 *       o el proximo cierre por vencimiento de #38, lo terminan de
 *       resolver.</li>
 *   <li>Cualquier otro estado no final y vigente ({@link EstadoProcesoReserva#HOLD_CREADO},
 *       {@link EstadoProcesoReserva#ESPERANDO_TELEFONO}, {@link EstadoProcesoReserva#TELEFONO_ENVIADO},
 *       {@link EstadoProcesoReserva#TELEFONO_RECHAZADO}): no tuvo ninguna
 *       llamada con efectos pendiente de confirmar, asi que simplemente
 *       sigue esperando (un hold que el usuario todavia no confirmo, o un
 *       proceso que espera el resultado por Kafka, que Kafka vuelve a
 *       entregar solo porque el offset no se confirmo).</li>
 * </ul>
 *
 * <p>Idempotente sin necesidad de una marca propia: solo revisa procesos
 * {@link ProcesoReservaRepositoryPort#listarNoFinales() no finales}, asi que
 * un proceso que esta recuperacion ya cerro o alineo no vuelve a aparecer en
 * la siguiente corrida.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecuperarProcesosPendientesUseCaseImpl implements RecuperarProcesosPendientesUseCase {

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;
    private final ConsultarReservaCatedraPort consultarReservaCatedraPort;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;
    private final CerrarProcesosVencidosUseCase cerrarProcesosVencidosUseCase;

    @Override
    public void recuperar() {
        int vencidos = cerrarProcesosVencidosUseCase.cerrarVencidos();

        List<ProcesoReserva> vigentes = procesoReservaRepositoryPort.listarNoFinales();
        log.info("Recuperacion al arrancar: {} vencido(s) cerrado(s), {} proceso(s) vigente(s) para revisar",
                vencidos, vigentes.size());

        int reconciliados = 0;
        int enEspera = 0;
        for (ProcesoReserva proceso : vigentes) {
            if (proceso.getEstado() == EstadoProcesoReserva.CONFIRMACION_INICIAL_ENVIADA) {
                reconciliados += reconciliar(proceso) ? 1 : 0;
            } else {
                log.debug("Proceso {} en estado {} sigue esperando (sin accion al arrancar)",
                        proceso.getReservationProcessId(), proceso.getEstado());
                enEspera++;
            }
        }

        log.info("Recuperacion al arrancar completa: {} vencido(s), {} reconciliado(s), {} en espera",
                vencidos, reconciliados, enEspera);
    }

    private boolean reconciliar(ProcesoReserva proceso) {
        Optional<ReservaCatedraEncontrada> encontrada;
        try {
            encontrada = consultarReservaCatedraPort.buscarPorReservationProcessId(
                    proceso.getExternalPatientId(), proceso.getReservationProcessId());
        } catch (CatedraServerErrorException | ResourceAccessException fallo) {
            log.warn("No se pudo reconciliar el proceso {} al arrancar (la catedra no respondio): se deja para un proximo intento ({})",
                    proceso.getReservationProcessId(), fallo.getMessage());
            return false;
        }

        if (encontrada.isEmpty()) {
            log.info("Proceso {} reconciliado al arrancar: la catedra todavia no tiene la confirmacion, sigue esperando",
                    proceso.getReservationProcessId());
            return false;
        }

        ProcesoReserva actualizado = transicionarProcesoReservaUseCase.transicionar(
                proceso, EstadoProcesoReserva.ESPERANDO_TELEFONO, Instant.now());
        procesoReservaRepositoryPort.guardar(actualizado);
        log.info("Proceso {} reconciliado al arrancar: la catedra ya tenia aplicada la confirmacion (status={})",
                proceso.getReservationProcessId(), encontrada.get().status());
        return true;
    }

}

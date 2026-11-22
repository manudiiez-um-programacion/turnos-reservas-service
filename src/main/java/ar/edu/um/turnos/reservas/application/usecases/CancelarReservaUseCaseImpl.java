package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import ar.edu.um.turnos.config.catedra.exception.CatedraServerErrorException;
import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException;
import ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException;
import ar.edu.um.turnos.reservas.domain.model.CancelacionReservaCatedra;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ReservaCatedraEncontrada;
import ar.edu.um.turnos.reservas.domain.model.SolicitudCancelacionReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.CancelarReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.CancelarReservaCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ConsultarReservaCatedraPort;
import ar.edu.um.turnos.reservas.domain.ports.out.ProcesoReservaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del caso de uso (#34): valida que el proceso exista y sea
 * del usuario, verifica que su estado local admita la cancelacion, y solo
 * entonces llama a la catedra.
 *
 * <p>Cancelar dos veces es idempotente (§12 de la referencia): si el proceso
 * ya esta {@link EstadoProcesoReserva#CANCELADA}, se devuelve su estado
 * actual sin volver a llamar a la catedra. Si todavia no llego a
 * {@link EstadoProcesoReserva#CONFIRMADA} —la unica transicion valida hacia
 * {@code CANCELADA}, ver {@link EstadoProcesoReserva#puedeTransicionarA}— se
 * rechaza localmente con {@link ReservaNoCancelableException}, sin gastar
 * una llamada a la catedra para un estado que ya se sabe no cancelable.
 *
 * <p>El estado local recien pasa a {@code CANCELADA} despues de que la
 * catedra confirma la cancelacion: un timeout no prueba que la operacion
 * haya fallado (§18.4), asi que no se cierra el proceso a ciegas.
 *
 * <p>{@code ReservaNoEncontradaException} y {@code ReservaOwnerMismatchException}
 * que llegan de la catedra, y un {@code ReservaNoCancelableException} propio
 * de la catedra (su estado ya no es cancelable), son inconsistencias: se
 * registran y se propagan sin tocar el estado local.
 *
 * <p>Un timeout o un error de infraestructura se reconcilia antes de repetir
 * la llamada (#36, §18.4): se busca el {@code reservationProcessId} en
 * {@code GET /api/appointments}. Como el proceso ya esta {@code CONFIRMADA}
 * para llegar hasta aca, la reserva en la catedra existe seguro; lo que hay
 * que decidir es si su {@code status} ya quedo {@code CANCELLED} (se alinea
 * el estado local con lo que informa la catedra, sin repetir la llamada) o
 * todavia no (se permite exactamente un reintento acotado).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelarReservaUseCaseImpl implements CancelarReservaUseCase {

    private static final String STATUS_CANCELADA_EN_CATEDRA = "CANCELLED";

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;
    private final CancelarReservaCatedraPort cancelarReservaCatedraPort;
    private final ConsultarReservaCatedraPort consultarReservaCatedraPort;
    private final TransicionarProcesoReservaUseCase transicionarProcesoReservaUseCase;

    @Override
    public ProcesoReserva cancelar(SolicitudCancelacionReserva solicitud) {
        Objects.requireNonNull(solicitud, "la solicitud de cancelacion no puede ser null");

        ProcesoReserva proceso = procesoReservaRepositoryPort.buscarPorReservationProcessId(solicitud.reservationProcessId())
                .orElseThrow(() -> new ReservaNoEncontradaException(solicitud.reservationProcessId()));

        if (!proceso.getUsuarioId().equals(solicitud.usuarioId())) {
            throw new ProcesoReservaAjenoException(solicitud.reservationProcessId(), solicitud.usuarioId());
        }

        if (proceso.getEstado() == EstadoProcesoReserva.CANCELADA) {
            log.info("Cancelacion repetida para la reserva {}: ya estaba cancelada", solicitud.reservationProcessId());
            return proceso;
        }

        if (proceso.getEstado() != EstadoProcesoReserva.CONFIRMADA) {
            throw new ReservaNoCancelableException(solicitud.reservationProcessId());
        }

        return intentarCancelar(proceso, solicitud, true);
    }

    private ProcesoReserva intentarCancelar(ProcesoReserva proceso, SolicitudCancelacionReserva solicitud, boolean permitirReconciliacion) {
        try {
            CancelacionReservaCatedra resultado = cancelarReservaCatedraPort.cancelar(proceso, solicitud.reason());
            proceso.setCancelledAt(resultado.cancelledAt());
            proceso.setCancelReason(solicitud.reason());
            proceso = transicionarProcesoReservaUseCase.transicionar(
                    proceso, EstadoProcesoReserva.CANCELADA, Instant.now());
            proceso = procesoReservaRepositoryPort.guardar(proceso);
            log.info("Reserva {} cancelada", solicitud.reservationProcessId());
            return proceso;
        } catch (ReservaNoEncontradaException | ReservaOwnerMismatchException | ReservaNoCancelableException inconsistencia) {
            log.warn("Inconsistencia cancelando la reserva {}: {}", solicitud.reservationProcessId(), inconsistencia.getMessage());
            throw inconsistencia;
        } catch (CatedraServerErrorException | ResourceAccessException timeout) {
            if (!permitirReconciliacion) {
                log.warn("Reintento de la cancelacion de la reserva {} agotado tras reconciliar: {}",
                        solicitud.reservationProcessId(), timeout.getMessage());
                throw timeout;
            }
            return reconciliarYReintentarCancelacion(proceso, solicitud, timeout);
        }
    }

    private ProcesoReserva reconciliarYReintentarCancelacion(
            ProcesoReserva proceso, SolicitudCancelacionReserva solicitud, RuntimeException timeout) {
        log.warn("Timeout o error de infraestructura cancelando la reserva {}: se reconcilia antes de reintentar ({})",
                solicitud.reservationProcessId(), timeout.getMessage());

        Optional<ReservaCatedraEncontrada> encontrada = buscarEnLaCatedra(solicitud.externalPatientId(), solicitud.reservationProcessId());
        if (encontrada.isPresent() && STATUS_CANCELADA_EN_CATEDRA.equals(encontrada.get().status())) {
            ReservaCatedraEncontrada reserva = encontrada.get();
            log.info("La catedra ya tenia cancelada la reserva {}: se alinea el estado local sin repetir la llamada",
                    solicitud.reservationProcessId());
            proceso.setCancelledAt(reserva.cancelledAt());
            proceso.setCancelReason(reserva.cancellationReason() != null ? reserva.cancellationReason() : solicitud.reason());
            proceso = transicionarProcesoReservaUseCase.transicionar(proceso, EstadoProcesoReserva.CANCELADA, Instant.now());
            return procesoReservaRepositoryPort.guardar(proceso);
        }

        log.info("La catedra todavia no tiene cancelada la reserva {}: se reintenta una vez, acotado", solicitud.reservationProcessId());
        return intentarCancelar(proceso, solicitud, false);
    }

    private Optional<ReservaCatedraEncontrada> buscarEnLaCatedra(String externalPatientId, String reservationProcessId) {
        try {
            return consultarReservaCatedraPort.buscarPorReservationProcessId(externalPatientId, reservationProcessId);
        } catch (CatedraServerErrorException | ResourceAccessException fallo) {
            log.warn("No se pudo reconciliar la reserva {} contra la catedra: {}", reservationProcessId, fallo.getMessage());
            return Optional.empty();
        }
    }

}

package ar.edu.um.turnos.reservas.application.usecases;

import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.reservas.application.exception.ProcesoReservaAjenoException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoCancelableException;
import ar.edu.um.turnos.reservas.application.exception.ReservaNoEncontradaException;
import ar.edu.um.turnos.reservas.application.exception.ReservaOwnerMismatchException;
import ar.edu.um.turnos.reservas.domain.model.CancelacionReservaCatedra;
import ar.edu.um.turnos.reservas.domain.model.EstadoProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.ProcesoReserva;
import ar.edu.um.turnos.reservas.domain.model.SolicitudCancelacionReserva;
import ar.edu.um.turnos.reservas.domain.ports.in.CancelarReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.in.TransicionarProcesoReservaUseCase;
import ar.edu.um.turnos.reservas.domain.ports.out.CancelarReservaCatedraPort;
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
 * haya fallado (§18.4), asi que no se cierra el proceso a ciegas. La
 * reconciliacion de esos timeouts queda para #36.
 *
 * <p>{@code ReservaNoEncontradaException} y {@code ReservaOwnerMismatchException}
 * que llegan de la catedra, y un {@code ReservaNoCancelableException} propio
 * de la catedra (su estado ya no es cancelable), son inconsistencias: se
 * registran y se propagan sin tocar el estado local.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelarReservaUseCaseImpl implements CancelarReservaUseCase {

    private final ProcesoReservaRepositoryPort procesoReservaRepositoryPort;
    private final CancelarReservaCatedraPort cancelarReservaCatedraPort;
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
        }
    }

}
